package org.luoyh.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

/**
 * 
 * @author luoyh(Roy)
 */
public abstract class ExportExcelUtils {

	private static HSSFWorkbook workbook = null;
	private static HSSFSheet sheet = null;
	// // spring mvc中获取request对象
	// private static HttpServletRequest request = ((ServletRequestAttributes)
	// RequestContextHolder.getRequestAttributes())
	// .getRequest();

	/**
	 * 生成Excel表格对象<br>
	 * 
	 * @param list
	 *            导出Excel数据List
	 * @param columnNames
	 *            表格标题数组
	 * @param columnValueKeys
	 *            获取数据的Keys数组
	 * @param sheetName
	 *            生成Sheet页的名称
	 * @return HSSFWorkbook对象
	 */
	public static HSSFWorkbook getWorkbook(List<Map<String, Object>> list, String[] columnNames, String[] columnValueKeys, String sheetName) {

		// 创建一个Excel文件
		workbook = new HSSFWorkbook();
		// 创建一个Sheet页
		sheet = workbook.createSheet(sheetName);
		// 设置单元格自适应宽度
		sheet.autoSizeColumn(1, true);

		// 创建sheet页的第1行，输出表头
		HSSFRow row = sheet.createRow(0);
		// 定义单元格
		HSSFCell cell = null;
		// 表头单元格样式
		HSSFCellStyle headStyle = getHeadStyle();
		for (int i = 0; i < columnNames.length; i++) {
			// 创建第i列
			cell = row.createCell(i);
			// 设置单元格数据
			cell.setCellValue(new HSSFRichTextString(columnNames[i]));
			// 给单元格设置样式
			cell.setCellStyle(headStyle);
		}

		// 内容单元格样式
		HSSFCellStyle bodyStyle = getBodyStyle();
		// 下面是输出各行的数据
		if (list != null && list.size() > 0) {
			for (int i = 0; i < list.size(); i++) {
				// 获取第i行数据
				Map<String, Object> map = list.get(i);
				// 创建sheet页的第i+1行（因为第一行为表头）
				row = sheet.createRow(i + 1);
				for (int j = 0; j < columnValueKeys.length; j++) {
					// 创建第j列
					cell = row.createCell(j);
					// 获取第j列的数据
					Object obj = map.get(columnValueKeys[j]);
					// 设置单元格数据
					if (null == obj || StringUtils.isBlank(obj.toString())) {
						cell.setCellValue("");
					} else {
						if (obj instanceof Integer) {
							cell.setCellValue(Integer.parseInt(obj.toString()));
						} else if (obj instanceof Double) {
							cell.setCellValue(Double.parseDouble(obj.toString()));
						} else if (obj instanceof Date) {
							// 强制转换为Date类型
							cell.setCellValue((Date) obj);
						} else if (obj instanceof String) {
							cell.setCellValue(obj.toString());
						} else {
							// 其他情况全部转换成文本类型显示
							cell.setCellValue(new HSSFRichTextString(obj.toString()));
						}
					}
					// 给单元格设置样式
					cell.setCellStyle(bodyStyle);
				}
			}
		}
		return workbook;
	}

	/**
	 * 下载生成的Excel文件（出现下载框）<br>
	 * 
	 * @param fileName
	 *            下载Excel文件名称
	 * @param workbook
	 *            Excel文件
	 * @param response
	 */
	public static byte[] downloadExcelFile(String fileName, HSSFWorkbook workbook) {

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
			workbook.write(baos);
			// 刷新缓冲区
			baos.flush();

			// 转换成字节
			byte[] content = baos.toByteArray();
			// 放进字节输入流中
			try (InputStream is = new ByteArrayInputStream(content);) {
				// 文件名编码
				fileName = URLEncoder.encode(fileName.concat(".xls"), "UTF-8").replace("+", "%20");

				byte[] ret = new byte[is.available()];
				int n = -1, i = 0;
				while ((n = is.read()) != -1) {
					ret[i++] = (byte) n;
				}
				return ret;
			} catch (UnsupportedEncodingException e) {
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 设置表头的单元格样式<br>
	 * 
	 * @return HSSFCellStyle对象
	 */
	private static HSSFCellStyle getHeadStyle() {

		/* 创建标题单元格样式 */
		HSSFCellStyle cellStyle = workbook.createCellStyle();
		/* 创建单元格字体 */
		HSSFFont font = workbook.createFont();
		// 设置字体类型
		font.setFontName("宋体");
		// 设置字体大小
		font.setFontHeightInPoints((short) 10);
		// 粗体显示
		font.setBold(true);
		// 给单元格设置字体格式
		cellStyle.setFont(font);

		// 设置单元格的背景颜色为淡蓝色
		cellStyle.setFillForegroundColor(HSSFColor.PALE_BLUE.index);
		cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		// 文本居中对齐
		cellStyle.setAlignment(HorizontalAlignment.CENTER);
		// 垂直方向居中对齐
		cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		// 单元格内容显示不下时自动换行
		// cellstyle.setWrapText(true);
		// 设置单元格边框为细线条
		cellStyle.setBorderLeft(BorderStyle.THIN);
		cellStyle.setBorderBottom(BorderStyle.THIN);
		cellStyle.setBorderRight(BorderStyle.THIN);
		cellStyle.setBorderTop(BorderStyle.THIN);
		return cellStyle;
	}

	/**
	 * 设置表体的单元格样式 <br>
	 * 
	 * @return HSSFCellStyle对象
	 */
	private static HSSFCellStyle getBodyStyle() {
		// 创建单元格样式
		HSSFCellStyle cellStyle = workbook.createCellStyle();
		// 设置单元格字体样式
		HSSFFont font = workbook.createFont();
		// 设置字体加粗
		font.setBold(true);
		// 设置字体类型
		font.setFontName("宋体");
		// 设置字体大小
		font.setFontHeight((short) 200);
		// 给单元格设置字体格式
		cellStyle.setFont(font);

		// 设置单元格居中对齐
		cellStyle.setAlignment(HorizontalAlignment.CENTER);
		// 设置单元格垂直居中对齐
		cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		// 创建单元格内容显示不下时自动换行
		cellStyle.setWrapText(true);
		// 设置单元格边框为细线条
		cellStyle.setBorderLeft(BorderStyle.THIN);
		cellStyle.setBorderBottom(BorderStyle.THIN);
		cellStyle.setBorderRight(BorderStyle.THIN);
		cellStyle.setBorderTop(BorderStyle.THIN);
		return cellStyle;
	}
}
