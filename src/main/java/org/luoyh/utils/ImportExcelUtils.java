package org.luoyh.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 
 * @author luoyh(Roy)
 */
@SuppressWarnings({ "unused", "deprecation" })
public abstract class ImportExcelUtils {

	/**
	 * 读取Excel文件<br>
	 * 
	 * @param filePath
	 *            文件路径
	 * @return List&lt;String[]&gt;对象
	 */
	public static List<String[]> readExcelFile(String filePath) throws Exception {
		InputStream is = null; // 输入流
		List<String[]> map = null;
		try {
			// 检查文件是否为Excel文件
			if (!isExcelFile(filePath)) {
				throw new Exception("文件格式错误(Excel格式)");
			}
			File file = new File(filePath);
			// 检查文件是否存在
			if (file == null || !file.exists()) {
				throw new Exception("文件不存在");
			}
			// 判断类型是2003格式还是2007格式
			boolean isExcel2003 = true;
			if (isExcel2007(filePath)) {
				isExcel2003 = false;
			}
			// 获取文件输入流
			is = new FileInputStream(file);
			// 根据Excel类型读取数据
			map = read(is, isExcel2003);
			// 关闭输入流
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return map;
	}

	/**
	 * 根据类型读取Excel文件<br>
	 * 
	 * @param is
	 *            输入流
	 * @param isExcel2003
	 *            是否2003Excel格式
	 * @return List&lt;String[]&gt;对象
	 * @throws Exception
	 */
	private static List<String[]> read(InputStream is, boolean isExcel2003) throws Exception {
		List<String[]> result = null;
		try {
			// 根据版本选择创建Workbook的方式
			Workbook wb = null;
			if (isExcel2003) {
				wb = new HSSFWorkbook(is);
			} else {
				wb = new XSSFWorkbook(is);
			}
			result = read(wb);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return result;
	}

	/**
	 * 读取Excel数据<br>
	 * 
	 * @param wb
	 *            Workbook对象
	 * @return List&lt;String[]&gt;对象
	 * @throws Exception
	 */
	private static List<String[]> read(Workbook wb) throws Exception {

		// 获取Excel文件第一个Sheet页
		Sheet sheet = wb.getSheetAt(0);
		// 获取总行数
		int rowNum = sheet.getLastRowNum();
		// 获取第一行
		Row row = sheet.getRow(0);
		// 得到总列数
		int colNum = 0;
		if (rowNum >= 1 && row != null) {
			colNum = row.getPhysicalNumberOfCells();
		}
		// 正文内容应该从第二行开始,第一行为表头的标题
		List<String[]> result = new ArrayList<String[]>();
		for (int i = 1; i <= rowNum; i++) {
			row = sheet.getRow(i);
			int j = 0;
			String[] tmp = new String[colNum];
			while (j < colNum) {
				tmp[j] = getCellFormatValue(row.getCell(j)).trim();
				j++;
			}
			result.add(tmp);
		}
		return result;
	}

	/**
	 * 获取单元格数据内容为字符串类型的数据<br>
	 * 
	 * @param cell
	 *            Excel单元格
	 * @return String 单元格数据内容
	 */
	private static String getStringCellValue(Cell cell) {
		String strCell = null;
		if (cell != null) {
			switch (cell.getCellType()) {
			case HSSFCell.CELL_TYPE_STRING:
				strCell = cell.getRichStringCellValue().getString();
				break;
			case HSSFCell.CELL_TYPE_NUMERIC:
				strCell = String.valueOf(cell.getNumericCellValue());
				break;
			case HSSFCell.CELL_TYPE_BOOLEAN:
				// 布尔类型（true : 0,false : 1）
				if (cell.getBooleanCellValue() == true) {
					strCell = "0";
				} else {
					strCell = "1";
				}
				break;
			case HSSFCell.CELL_TYPE_BLANK:
				strCell = "";
				break;
			default:
				strCell = "";
				break;
			}
		} else {
			strCell = "";
		}
		return strCell;
	}

	/**
	 * 根据HSSFCell类型设置数据<br>
	 * 
	 * @param cell
	 *            Excel单元格对象
	 * @return 返回String格式数据
	 * @throws Exception
	 */
	private static String getCellFormatValue(Cell cell) throws Exception {
		String cellvalue = null;
		if (cell != null) {
			// 判断当前Cell的Type
			switch (cell.getCellType()) {
			case HSSFCell.CELL_TYPE_STRING: // 字符串
				cellvalue = cell.getStringCellValue();
				break;
			case HSSFCell.CELL_TYPE_NUMERIC: // 数值
			case HSSFCell.CELL_TYPE_FORMULA: // 公式
				// 判断当前的cell是否为Date类型
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					cellvalue = DateUtils.date2String(cell.getDateCellValue());
				} else { // 如果是纯数字
					// 取得当前Cell的数值
					cellvalue = new DecimalFormat("###.###").format(cell.getNumericCellValue());// 格式化为浮点数
				}
				break;
			case HSSFCell.CELL_TYPE_BOOLEAN: // 布尔
				// 布尔类型（true : 0,false : 1）
				if (cell.getBooleanCellValue() == true) {
					cellvalue = "0";
				} else {
					cellvalue = "1";
				}
				break;
			case HSSFCell.CELL_TYPE_BLANK: // 空白
				cellvalue = "";
				break;
			case HSSFCell.CELL_TYPE_ERROR:
				throw new Exception("非法字符");
			default:
				throw new Exception("未知类型");
			}
		} else {
			cellvalue = "";
		}
		return cellvalue;
	}

	/**
	 * 验证是否为excel文件<br>
	 * 
	 * @param filePath
	 *            文件完整路径
	 * @return true:是，false:否
	 */
	public static boolean isExcelFile(String filePath) {
		// 检查文件名是否为空或者是否是Excel格式的文件
		if (filePath == null || !(isExcel2003(filePath) || isExcel2007(filePath))) {
			return false;
		}
		return true;
	}

	/**
	 * 是否是2003的excel，返回true是2003<br>
	 * 
	 * @param filePath
	 *            文件完整路径
	 * @return true: 2003格式，false: 非2003格式
	 */
	private static boolean isExcel2003(String filePath) {
		if (StringUtils.isBlank(filePath)) {
			return false;
		}
		// 校验规则
		Pattern pt = Pattern.compile("^.+\\.(?i)(xls)$");
		// 匹配规则
		Matcher mt = pt.matcher(filePath);
		return mt.matches();
	}

	/**
	 * 是否是2007的excel，返回true是2007<br>
	 * 
	 * @param filePath
	 *            文件完整路径
	 * @return true: 2007格式，false: 非2007格式
	 */
	private static boolean isExcel2007(String filePath) {
		if (StringUtils.isBlank(filePath)) {
			return false;
		}
		// 校验规则
		Pattern pt = Pattern.compile("^.+\\.(?i)(xlsx)$");
		// 匹配规则
		Matcher mt = pt.matcher(filePath);
		return mt.matches();
	}

	public static void main(String[] args) {

		// 对读取Excel表格内容测试
		List<String[]> list = null;
		try {
			list = ImportExcelUtils.readExcelFile("E:\\test2.xlsx");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		System.out.println("获得Excel表格的内容:");
		for (String[] str : list) {
			for (int i = 0; i < str.length; i++) {
				System.out.print(str[i] + "---");
			}
			System.out.println();
		}
	}

}
