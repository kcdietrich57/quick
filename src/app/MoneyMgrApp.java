package app;

import java.util.Scanner;

import qif.data.QifDom;
import qif.importer.QifDomReader;
import qif.ui.MainFrame;

public class MoneyMgrApp {
	public static Scanner scn;
	public static QifDom dom;

	public static void main(String[] args) {
		MoneyMgrApp.scn = new Scanner(System.in);
		MoneyMgrApp.dom = QifDomReader.loadDom(new String[] { "qif/DIETRICH.QIF" });

		MainFrame.createUI();
	}
}
