package qif.data;

import java.io.File;

public class QifDom {
	public static File qifDir = null;

	public static boolean verbose = true;

	/** Pay attention to version of the loaded QIF file format */
	public static int loadedStatementsVersion = -1;
}
