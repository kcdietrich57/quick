package qif.data;

import java.io.File;
import java.util.Properties;

public class QifDom {
	public static File qifDir = null;

	public static boolean verbose = false;

	/** Pay attention to version of the loaded QIF file format */
	public static int loadedStatementsVersion = -1;

	public static Properties qifProperties = null;
}
