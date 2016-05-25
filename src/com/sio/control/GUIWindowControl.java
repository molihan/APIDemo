package com.sio.control;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
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
	public final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	public final static Pattern pattern = Pattern.compile("[0-9]{4}/[0-9]{2}/[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}");
	private static final String SEND = "ESLSend";
	private static final String LED = "ESLLed";
	private static final String BROADCAST = "ESLBroadcast";
	private static final String KEY = "ESLKey";
	private static final String RESET = "ESLReset";
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
								solveFile(chosen);								
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
		
		if(lines.size()>0){								//	������������
			Packer packer = new DefaultUDPA1Pack();		//	��ʼ����
			
			for(int x=0; x<lines.size(); x++){			//	��һ�� read line
				String line = lines.get(x);
				String prev_line = null;
				
				if(x != 0){								//	��Ϊ��һ�� 		not first line
					prev_line = lines.get(x-1);			//	��ȡ����һ��	then load the previous line.
				}
				
				String[] args = line.split(" ");		//	������������ �жϷ�ֹ�쳣
				if(args.length > 2){		
					if(args[0].equalsIgnoreCase(RESET) && args.length == 3){						//	��������
						solveReset(args[1], args[2]);
						if(x == lines.size()-1){
							return;
						} else {
							continue;
						}
					}
					
					String mac = args[1];
					if(x == 0){							//	��Ϊ��һ��
						packer.setHead(mac, new Random().nextLong(), null);		//	��ʼ��ͷ
					}
					if(prev_line != null &&  prev_line.split(" ").length > 2){	//	������һ�д���������MAC
						String prev_mac = prev_line.split(" ")[1];
						if(mac.equalsIgnoreCase(prev_mac)){						//	��һ����ͬ
							
						} else {												//	��һ�в�ͬ
							sendPack(prev_mac, packer);
							packer = new DefaultUDPA1Pack();					//	���ð�
							packer.setHead(mac, new Random().nextLong(), null);	//	��ʼ��ͷ
						}
					}
					//���������ݼӵ�����
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
							String ymd_hms = ymd + " " + hms;
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
				} else {								//	�����������������ļ����ڴ����С�
					if(x == 0){
						System.out.println("input format error!!!.");
						return;
					}
					lines.remove(x);
					continue;
				}
			}											//	�������һ��
			
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
}
