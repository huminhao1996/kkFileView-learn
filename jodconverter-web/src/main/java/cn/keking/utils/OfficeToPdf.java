package cn.keking.utils;

import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Office -> Pdf 工具类
 * @author yudian-it
 */
@Component
public class OfficeToPdf {

    // 文件类型转换 工具类
    private final ConverterUtils converterUtils;

    public OfficeToPdf(ConverterUtils converterUtils) {
        this.converterUtils = converterUtils;
    }

    public void openOfficeToPDF(String inputFilePath, String outputFilePath) {
        office2pdf(inputFilePath, outputFilePath);
    }

    /**
     * 文件类型转换
     * @param inputFile
     * @param outputFilePath_end
     * @param converter
     */
    public static void converterFile(File inputFile,
                                     String outputFilePath_end,
                                     OfficeDocumentConverter converter) {
        File outputFile = new File(outputFilePath_end);
        // 假如目标路径不存在,则新建该路径
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }
        // 核心方法 文件类型转换
        converter.convert(inputFile, outputFile);
    }

    /**
     * office -> pdf
     * 例如 D:\kkFileview\b937eb1a-e02f-431d-bba9-bd9a4b664fbc.docx -> D:\kkFileview\1595314467960.pdf
     * TODO: 2020/7/30 优化 能不能分片转换加快效率
     *
     * @param inputFilePath  从远程端下载到kkfile服务端的文件路径 例: D:\kkFileview\b937eb1a-e02f-431d-bba9-bd9a4b664fbc.docx
     * @param outputFilePath 文件转换后的路径 例: D:\kkFileview\1595314467960.pdf
     */
    public void office2pdf(String inputFilePath, String outputFilePath) {
        // 获取 office 转换器
        OfficeDocumentConverter converter = converterUtils.getDocumentConverter();
        if (null != inputFilePath) {
            File inputFile = new File(inputFilePath);
            // 判断目标文件路径是否为空
            if (null == outputFilePath) {
                // 转换后的文件路径
                String outputFilePath_end = getOutputFilePath(inputFilePath);
                if (inputFile.exists()) {
                    // 找不到源文件, 则返回
                    converterFile(inputFile, outputFilePath_end,converter);
                }
            } else {
                if (inputFile.exists()) {
                    // 找不到源文件, 则返回
                    converterFile(inputFile, outputFilePath, converter);
                }
            }
        }
    }

    public static String getOutputFilePath(String inputFilePath) {
        return inputFilePath.replaceAll("."+ getPostfix(inputFilePath), ".pdf");
    }

    public static String getPostfix(String inputFilePath) {
        return inputFilePath.substring(inputFilePath.lastIndexOf(".") + 1);
    }

}
