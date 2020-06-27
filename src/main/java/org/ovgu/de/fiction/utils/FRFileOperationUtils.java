package org.ovgu.de.fiction.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.maltparser.core.helper.HashMap;

/**
 * @author Suhita 
 */

/**
 * This is utility class for file operations
 */
public class FRFileOperationUtils {

	private static Logger LOG = Logger.getLogger(FRFileOperationUtils.class);

	/**
	 * @return
	 * @throws IOException
	 *             The method fetches file names present in the folder configured in the properties
	 *             file
	 */
	public static List<Path> getFileNames(String folderName) throws IOException {
		try (Stream<Path> paths = Files.walk(Paths.get(folderName))) {
			List<Path> filePathList = paths.filter(Files::isRegularFile).collect(Collectors.toList());
			return filePathList;
		}
	}

	/**
	 * @param src
	 * @param des
	 * @return The methods copies files from folder location passed in the method
	 */
	public static boolean copyFile(Path src, Path des) {
		File source = new File(src.toString());
		File dest = new File(des.toString());
		try {
			FileUtils.copyFileToDirectory(source, dest);
			return true;
		} catch (IOException e) {
			LOG.error("Files could not be copied -" + e.getMessage());
		}
		return false;
	}

	/**
	 * @param path
	 * @return
	 */
	public static String readFile(String path) {
		File file = new File(path);
		try {
			byte[] bytes = Files.readAllBytes(file.toPath());
			return new String(bytes);
		} catch (IOException e) {
			LOG.error("Files could not be read -" + e.getMessage());
		}
		return "";
	}
	
	public static Map<String, double[]> readCsvForBOW() {
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		Map<String, double[]> valueMap = new HashMap<String, double[]>();

		try {
			br = new BufferedReader(new FileReader(FRGeneralUtils.getPropertyVal("file.bow.feature")));
			br.readLine();
			while ((line = br.readLine()) != null) {

				String[] valueArray = line.split(cvsSplitBy);
				double[] doubleValueArray = new double[154474];
				for (int i = 1; i < valueArray.length; i++) {
					doubleValueArray[i - 1] = Double.valueOf(valueArray[i]);
				}
				valueMap.put(valueArray[0], doubleValueArray);
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return valueMap;
	}

	public static List<String> readCsvForBookNames() {
		BufferedReader br = null;
		String line = "";
		List<String> bookList = new ArrayList<String>();

		try {
			br = new BufferedReader(new FileReader(FRGeneralUtils.getPropertyVal("file.book.names")));
			br.readLine();
			while ((line = br.readLine()) != null) {
					bookList.add(line);
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bookList;
	}

}
