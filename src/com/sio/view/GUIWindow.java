package com.sio.view;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.AbstractListModel;
import javax.swing.JButton;

import com.sio.control.GUIWindowControl;
import com.sio.model.AbstractAccessPoint;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Set;
import java.awt.FlowLayout;
import java.awt.Component;
import javax.swing.Box;
import java.awt.Toolkit;
import java.awt.Dialog.ModalExclusionType;

public class GUIWindow extends JFrame {
	private JList<String> list;
	private JPanel contentPane;

//	controller
	private GUIWindowControl control = GUIWindowControl.instance;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUIWindow frame = new GUIWindow();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public GUIWindow() {
		setResizable(false);
		setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
		setIconImage(Toolkit.getDefaultToolkit().getImage(GUIWindow.class.getResource("/res/icon-if-3232.png")));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 250, 200);
		setTitle("ifLabel API GUI");
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout(0, 0));
		list = new JList<>();
		JScrollPane scrollPane = new JScrollPane();
		panel.add(scrollPane);
		scrollPane.setViewportView(list);
		
		Component horizontalStrut = Box.createHorizontalStrut(20);
		panel.add(horizontalStrut, BorderLayout.WEST);
		
		
		JPanel panel_1 = new JPanel();
		contentPane.add(panel_1, BorderLayout.SOUTH);
		
		JButton btnNewButton = new JButton("Open");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				 onButtonClicked(e);
			}
		});
		panel_1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		panel_1.add(btnNewButton);
		
		Component horizontalStrut_1 = Box.createHorizontalStrut(20);
		contentPane.add(horizontalStrut_1, BorderLayout.EAST);
		setLocationRelativeTo(null);
	}

	public void setList(String[] rows){
		list.removeAll();
		DefaultListModel<String> model = new DefaultListModel<>();
		for(String row : rows){
			model.addElement(row);
		}
		list.setModel(model);
	}
	
	public void onButtonClicked(final ActionEvent action){
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				control.onOpenClickCallBack(action);				
			}
		}).start();
		
	}

	public void setList(Set<AbstractAccessPoint> accessPoints) {
		list.removeAll();
		DefaultListModel<String> model = new DefaultListModel<>();
		for(AbstractAccessPoint accessPoint : accessPoints){
			model.addElement(accessPoint.getIp());
		}
		list.setModel(model);
	}
}
