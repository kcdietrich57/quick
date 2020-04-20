package app;

import java.io.File;
import java.util.Properties;

import moneymgr.model.MoneyMgrModel;

/** Global information and functions */
public class QifDom {
	/** The data model */
	public static final MoneyMgrModel theModel = new MoneyMgrModel();

	/** The location of the data files */
	public static File qifDir = null;

	public static boolean verbose = false;

	/** Version of the loaded/saved file format */
	public static int loadedStatementsVersion = -1;

	/** Properties map for persisting state and appearance info */
	public static Properties qifProperties = null;
}
