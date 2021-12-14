package cn.keking.utils;

import cn.keking.config.ConfigConstants;
import cn.keking.hutool.URLUtil;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.model.ReturnResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 文件下载 工具类
 * @author yudian-it
 */
@Component
public class DownloadUtils {

    private final Logger logger = LoggerFactory.getLogger(DownloadUtils.class);

    private final String fileDir = ConfigConstants.getFileDir();

    private final FileUtils fileUtils;

    public DownloadUtils(FileUtils fileUtils) {
        this.fileUtils = fileUtils;
    }

    private static final String URL_PARAM_FTP_USERNAME = "ftp.username";
    private static final String URL_PARAM_FTP_PASSWORD = "ftp.password";
    private static final String URL_PARAM_FTP_CONTROL_ENCODING = "ftp.control.encoding";

    /**
     * 下载远程端文件到 KKFile服务器
     * @param fileAttribute fileAttribute
     * @param fileName 文件名
     * @return 本地文件绝对路径
     */
    public ReturnResponse<String> downLoad(FileAttribute fileAttribute, String  fileName) {
        String urlStr = fileAttribute.getUrl(); // 文件下载地址或接口
        String type = fileAttribute.getSuffix(); // 文件后缀
        ReturnResponse<String> response = new ReturnResponse<>(0, "下载成功!!!", "");
        // uuid 作为转换后的新名称
        UUID uuid = UUID.randomUUID();
        if (null == fileName) {
            fileName = uuid+ "."+type;
        } else {
            // 文件后缀不一致时，以type为准(针对simText【将类txt文件转为txt】)
            fileName = fileName.replace(fileName.substring(fileName.lastIndexOf(".") + 1), type);
        }

        // 下载远程端文件到服务器后的路径
        String realPath = fileDir + fileName;
        File dirFile = new File(fileDir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }

        // 下载远程端文件 到服务器
        try {
            //应用服务器提供的文件下载接口 例: http://47.111.75.34:8885/jd/file/download?url=/programes/jdxm/excelDemo/1595314467960.docx&fullfilename=1595314467960.docx
            URL url = new URL(urlStr);
            OutputStream os = new FileOutputStream(new File(realPath));
            //判断是 http请求还是 ftp
            if (url.getProtocol() != null && url.getProtocol().toLowerCase().startsWith("http")) {
                // 核心方法:将远程端文件以流的形式写入 kkfile服务端的文件中
                saveToOutputStreamFromUrl(urlStr, os);
            } else if (url.getProtocol() != null && "ftp".equals(url.getProtocol().toLowerCase())) {
                String ftpUsername = fileUtils.getUrlParameterReg(fileAttribute.getUrl(), URL_PARAM_FTP_USERNAME);
                String ftpPassword = fileUtils.getUrlParameterReg(fileAttribute.getUrl(), URL_PARAM_FTP_PASSWORD);
                String ftpControlEncoding = fileUtils.getUrlParameterReg(fileAttribute.getUrl(), URL_PARAM_FTP_CONTROL_ENCODING);
                FtpUtils.download(fileAttribute.getUrl(), realPath, ftpUsername, ftpPassword, ftpControlEncoding);
            } else {
                response.setCode(1);
                response.setContent(null);
                response.setMsg("url不能识别url" + urlStr);
            }
            response.setContent(realPath);
            response.setMsg(fileName);
            // 如果文件格式为 txt的话,转成utf-8
            if(FileType.simText.equals(fileAttribute.getType())){
                convertTextPlainFileCharsetToUtf8(realPath);
            }
            return response;
        } catch (IOException e) {
            logger.error("文件下载失败，url：{}", urlStr, e);
            response.setCode(1);
            response.setContent(null);
            if (e instanceof FileNotFoundException) {
                response.setMsg("文件不存在!!!");
            } else {
                response.setMsg(e.getMessage());
            }
            return response;
        }
    }

    /**
     * 将远程端文件以流的形式写入 kkfile服务端的文件中
     *
     * @param urlStr   远程端地址 例: http://47.111.75.34:8885/jd/file/download?url=/programes/jdxm/excelDemo/1595314467960.docx&fullfilename=1595314467960.docx
     * @param os       输出流
     * @return
     * @throws IOException
     */
    public boolean saveToOutputStreamFromUrl(String urlStr, OutputStream os) throws IOException {
        // 1. 获取url对应的流
        InputStream is = getInputStreamFromUrl(urlStr);
        if (is != null) {
            copyStream(is, os);
        } else {
            urlStr = URLUtil.normalize(urlStr, true, true);
            is = getInputStreamFromUrl(urlStr);
            if (is != null) {
                copyStream(is, os);
            } else {
                os.close();
                return false;
            }
        }
        return true;
    }

    /**
     * 获取url对应的流
     *
     * @param urlStr   远程端地址 例: http://47.111.75.34:8885/jd/file/download?url=/programes/jdxm/excelDemo/1595314467960.docx&fullfilename=1595314467960.docx
     * @return
     * @throws IOException
     */
    private InputStream getInputStreamFromUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            URLConnection connection = url.openConnection();
            if (connection instanceof HttpURLConnection) {
                connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            }
            return connection.getInputStream();
        } catch (IOException e) {
            logger.warn("连接url异常：url：{}", urlStr);
            return null;
        }
    }

    private void copyStream(InputStream is, OutputStream os) throws IOException {
        // TODO: 2021/5/9 这里是否可以增加byte数组大小,提升IO效率
        byte[] bs = new byte[1024];
        int len;
        while (-1 != (len = is.read(bs))) {
            os.write(bs, 0, len);
        }
        is.close();
        os.close();
    }

  /**
   * 转换文本文件编码为utf8
   * 探测源文件编码,探测到编码切不为utf8则进行转码
   * @param filePath 文件路径
   */
  private static void convertTextPlainFileCharsetToUtf8(String filePath) throws IOException {
    File sourceFile = new File(filePath);
    if(sourceFile.exists() && sourceFile.isFile() && sourceFile.canRead()) {
      String encoding = null;
      try {
        FileCharsetDetector.Observer observer = FileCharsetDetector.guessFileEncoding(sourceFile);
        // 为准确探测到编码,不适用猜测的编码
        encoding = observer.isFound()?observer.getEncoding():null;
        // 为准确探测到编码,可以考虑使用GBK  大部分文件都是windows系统产生的
      } catch (IOException e) {
        // 编码探测失败,
        e.printStackTrace();
      }
      if(encoding != null && !"UTF-8".equals(encoding)){
        // 不为utf8,进行转码
        File tmpUtf8File = new File(filePath+".utf8");
        Writer writer = new OutputStreamWriter(new FileOutputStream(tmpUtf8File), StandardCharsets.UTF_8);
        Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFile),encoding));
        char[] buf = new char[1024];
        int read;
        while ((read = reader.read(buf)) > 0){
          writer.write(buf, 0, read);
        }
        reader.close();
        writer.close();
        // 删除源文件
        sourceFile.delete();
        // 重命名
        tmpUtf8File.renameTo(sourceFile);
      }
    }
  }
}
