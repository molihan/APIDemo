package com.sio.launcher;

import java.util.Set;

import com.sio.control.GUIWindowControl;
import com.sio.model.AbstractAccessPoint;
import com.sio.model.DeviceUtility;
import com.sio.model.UtilityProvider;
import com.sio.object.APIService;

public class Launch{
	public static Set<AbstractAccessPoint> access_points;
	
	public Launch() {
		//start esl server
		APIService service = new APIService();
		//get access point list
		DeviceUtility util = UtilityProvider.getUtility();
		access_points = util.getAccessPoints();
		//create GUI
		GUIWindowControl gui = new GUIWindowControl();
		gui.show();
	}
	
	public static void main(String[] args) {
		new Launch();
	}
}
