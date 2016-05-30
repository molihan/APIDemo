package com.sio.control;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.sio.graphics.DefaultImageCaster;
import com.sio.graphics.ImageCaster;
import com.sio.graphics.PixelMatrixTemplate;
import com.sio.graphics.StaticTextElement;
import com.sio.graphics.Template;
import com.sio.model.AbstractAccessPoint;
import com.sio.model.DefaultUDPA1Pack;
import com.sio.model.DefaultUDPTag;
import com.sio.model.DeviceUtility;
import com.sio.model.Packer;
import com.sio.model.Tag;
import com.sio.model.UtilityProvider;
import com.sio.model.WirelessTag;
import com.sio.util.DefaultTemplateFactory;
import com.sio.util.TemplateFactory;
import com.sio.view.GUIWindow;

public class GUIWindowControl {
	public final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss");
	public final static Pattern pattern = Pattern.compile("[0-9]{4}/[0-9]{2}/[0-9]{2},[0-9]{2}:[0-9]{2}:[0-9]{2}");
	private static final String SEND = "ESLSEND";
	private static final String LED = "ESLLED";
	private static final String BROADCAST = "ESLBROADCAST";
	private static final String KEY = "ESLKEY";
	private static final String RESET = "ESLRESET";
	private static final String SPLIT_COMMA = ",";
	private static final String MAC_RESET_FILENAME = "reset_list.txt";
	private boolean hasReset = false;
	private File mac_file;
//	view
	private GUIWindow window;
//	instance
	public static GUIWindowControl instance;
	
	public GUIWindowControl() {
		instance = this;
	}

	public void show(){
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				window = new GUIWindow();
				window.setVisible(true);
			}
		});
		new Thread(new APAKeeper()).start();
		
	}
	
	public void onOpenClickCallBack(ActionEvent e){
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				JFileChooser chooser = new JFileChooser(new File("./"));
				chooser.setVisible(true);
				FileFilter filter = new FileNameExtensionFilter("an ASCII text file(.bat .txt .log)", "bat","txt","log");
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.setFileFilter(filter);
				chooser.setAcceptAllFileFilterUsed(false);
				int returnInv  = chooser.showOpenDialog(window);
				if(returnInv == JFileChooser.APPROVE_OPTION){
					final File chosen = chooser.getSelectedFile();
					if(chosen != null){
						new Thread(new Runnable() {
							
							@Override
							public void run() {
								hasReset = false;
								mac_file = new File(chosen.getParentFile(),MAC_RESET_FILENAME);
								initMacFile();
								solveFile(chosen);	
								if(hasReset){
									printDate();
								}
							}
						}).start();
					}
				}
			}
		});
		
	}
	
	private void solveFile(File file){
		ArrayList<String> lines = new ArrayList<>();
		try(
			FileReader reader = new FileReader(file);
			BufferedReader bReader = new BufferedReader(reader);
		){
			String line = null;
			while((line = bReader.readLine())!= null && line.length() > 0){
				lines.add(line);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(lines.size()>0){								//	当有数据请求
			Packer packer = new DefaultUDPA1Pack();		//	初始化包

			for(int x=0; x<lines.size(); x++){			//	读一行 read line
				String line = lines.get(x);
				String prev_line = null;

				if(x != 0){								//	不为第一行 		not first line
					prev_line = lines.get(x-1);			//	则取出上一行	then load the previous line.
				}
				line = line.toUpperCase().replaceAll("，", ",");
				lines.set(x, line);
				String[] args = line.split(SPLIT_COMMA);		//	参数大于两个 判断防止异常
				for(int i=0; i<args.length; i++){
					args[i] = args[i].trim();
				}
				if(args.length > 2){
					if(args[0].equalsIgnoreCase(RESET) && args.length == 3){						//	重置命令
						hasReset = true;
						solveReset(args[1], args[2]);
						if(x == lines.size()-1){
							return;
						} else {
							continue;
						}
					}
					
					String mac = args[1];
					if(x == 0){							//	若为第一行
						packer.setHead(mac, new Random().nextLong(), null);		//	初始化头
					}
					if(prev_line != null &&  prev_line.split(SPLIT_COMMA).length > 2){	//	参与上一行处理，有且有MAC
						String prev_mac = prev_line.split(SPLIT_COMMA)[1].trim();
						if(mac.equalsIgnoreCase(prev_mac)){						//	上一行相同
							
						} else {												//	上一行不同
							sendPack(prev_mac, packer);
							packer = new DefaultUDPA1Pack();					//	重置包
							packer.setHead(mac, new Random().nextLong(), null);	//	初始化头
						}
					}
					//把请求内容加到包内
					switch(args[0]){
					case SEND:
						if(!new File(args[2]).exists()){
							System.out.println("Image not exist [url] -> " + args[2]);
						}
						
						try(FileInputStream in = new FileInputStream(args[2])){
							Set<AbstractAccessPoint> aps = UtilityProvider.getUtility().getAccessPoints();
							for(AbstractAccessPoint ap : aps){
								if(ap.contains(mac)){
									WirelessTag wTag = ap.getTag(mac);
									Tag tag = wTag.getTag();
									ImageCaster caster = new DefaultImageCaster();
									byte[] data = caster.cast(ImageIO.read(in), tag.model());
									in.close();
									packer.setData(Packer.ORDER_SEND_BW, data);
									break;
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						if(args.length == 5){
							String ymd = args[3];
							String hms = args[4];
							String ymd_hms = ymd + SPLIT_COMMA + hms;
							Matcher matcher = pattern.matcher(ymd_hms);
							if(matcher.matches()){
								try {
									Date date = dateFormat.parse(ymd_hms);
									packer.setTimer(date);
								} catch (ParseException e) {
									e.printStackTrace();
								}
								
							}
						}
						break;
					case KEY:
						packer.setData(Packer.ORDER_KEY, new byte[]{0x01});
						break;
					case BROADCAST:
						packer.setData(Packer.ORDER_BROADCAST, new byte[]{(byte) (Integer.parseInt(args[2])&0xFF)});
						break;
					case LED:
						if(args.length == 18){ 
							ByteBuffer buf = ByteBuffer.allocate(16);
							for(int led=2; led<args.length; led++){
								buf.put((byte) (Integer.parseInt(args[led])&0xFF));
							}
							packer.setData(Packer.ORDER_LED, buf.array());
						} else {
							System.out.println("input format error!!!.");
						}
						break;
					}
					if(x == lines.size()-1){
						sendPack(mac, packer);
					}
				} else {								//	参数不大于两个，文件存在错误行。
					if(x == 0){
						System.out.println("input format error!!!.");
						return;
					}
					lines.remove(x);
					continue;
				}
			}											//	读完最后一行
			
		}
		
	}
	
	private synchronized void solveReset(String type, String power) {
		ArrayList<String> macs = new ArrayList<>();
		Set<AbstractAccessPoint> aps = UtilityProvider.getUtility().getAccessPoints();
		for(AbstractAccessPoint ap : aps){
			Collection<WirelessTag> wTags = new HashSet<>();
			wTags.addAll(ap.getTags());
			for(WirelessTag wTag : wTags){
				if(!macs.contains(wTag.getMac())){
					macs.add(wTag.getMac());
					Tag tag = wTag.getTag();
					if(Integer.toString(tag.model()).equals(type)){
						if(tag.signal() <= Integer.parseInt(power)){
							BufferedImage image = new BufferedImage(296, 128, BufferedImage.TYPE_BYTE_BINARY);
							Graphics2D g2d = (Graphics2D) image.getGraphics();
							g2d.setColor(Color.WHITE);
							g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
							g2d.setColor(Color.BLACK);
							g2d.drawString(wTag.getMac(), 20, 30);
							g2d.dispose();
							ImageCaster caster = new DefaultImageCaster();
							Packer packer = new DefaultUDPA1Pack();
							packer.setHead(wTag.getMac(), new Random().nextLong(), null);
							packer.setData(DefaultUDPA1Pack.ORDER_SEND_BW,caster.cast(image, tag.model()));
							wTag.write(packer.getPack());
							writeMacFile(wTag.getMac());
						}
					}
				}
			}
			wTags.clear();
		}
		
	}

	private void sendPack(String mac, Packer packer){
		Set<AbstractAccessPoint> aps = UtilityProvider.getUtility().getAccessPoints();
		for(AbstractAccessPoint ap : aps){
			if(ap.contains(mac)){
				WirelessTag wTag = ap.getTag(mac);
				wTag.write(packer.getPack());
			}
			break;
		}
	}
	
	private class APAKeeper implements Runnable{

		@Override
		public void run() {
			while(true){
				if (window != null){
					DeviceUtility util = UtilityProvider.getUtility();
					window.setList(util.getAccessPoints());
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	public void onTestClickCallBack() {
		TemplateFactory fac = DefaultTemplateFactory.instance;
		PixelMatrixTemplate template = (PixelMatrixTemplate) fac.createTemplate(TemplateFactory.PIXEL_TEMPLATE);
		template.setWidth(250);
		template.setHeight(128);
		
		//add element
		StaticTextElement se = new StaticTextElement(template);
		se.setX(20);
		se.setY(20);
		se.setContent("123456");
		template.addElement(se);
		try(FileOutputStream out = new FileOutputStream(new File("./test.bmp"))){
			ImageIO.write(template.getImage(), "bmp", out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initMacFile(){
		if(mac_file.exists()){
			mac_file.delete();
			try {
				mac_file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				mac_file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void writeMacFile(String mac){
		try(
				FileWriter fw = new FileWriter(mac_file,true);
				BufferedWriter out = new BufferedWriter(fw);
			){
				out.write(mac);
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		
	}
	
	private void printDate(){
		try(
				FileWriter fw = new FileWriter(mac_file,true);
				BufferedWriter out = new BufferedWriter(fw);
			){
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(new Date());
				out.write(calendar.toString());
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
}
