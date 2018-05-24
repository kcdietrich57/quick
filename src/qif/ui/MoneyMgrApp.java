package qif.ui;

import java.util.Scanner;

import qif.data.QifDom;
import qif.data.QifDomReader;

public class MoneyMgrApp {
	public static Scanner scn;
	public static QifDom dom;

	public static void main(String[] args) {
		MoneyMgrApp.scn = new Scanner(System.in);
		MoneyMgrApp.dom = QifDomReader.loadDom(new String[] { "qif/DIETRICH.QIF" });

		MainFrame.createUI();
	}
}
