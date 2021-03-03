package cn.keking.config;

import org.artofsolving.jodconverter.office.OfficeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;

/**
 * 配置文件参数
 * @author: chenjh
 * @since: 2019/4/10 17:22
 */
@Component
public class ConfigConstants {

    private static Boolean CACHE_ENABLED;   // 是否开启缓存
    private static String[] SIM_TEXT = {};  // 文本类型
    private static String[] MEDIA = {};     // 媒体类型
    private static String OFFICE_PREVIEW_TYPE; // 页面预览类型 默认为图片(image)，可配置为pdf
    private static String FTP_USERNAME;
    private static String FTP_PASSWORD;
    private static String FTP_CONTROL_ENCODING;
    private static String BASE_URL; // kkfileview服务器地址
    private static String FILE_DIR = OfficeUtils.getHomePath() + File.separator + "file" + File.separator;   // 文件上传目录 默认为: 当前项目绝对路径\jodconverter-web\src\main\ 例: D:\ideaProject\文件在线预览\kk_file_preview\jodconverter-web\src\main\
    private static CopyOnWriteArraySet<String> TRUST_HOST_SET;  //信任站点
    private static String PDF_DOWNLOAD_DISABLE; // 文件预览的pdf是否允许下载

    public static final String DEFAULT_CACHE_ENABLED = "true";
    public static final String DEFAULT_TXT_TYPE = "txt,html,htm,asp,jsp,xml,json,properties,md,gitignore,log,java,py,c,cpp,sql,sh,bat,m,bas,prg,cmd";
    public static final String DEFAULT_MEDIA_TYPE = "mp3,wav,mp4,flv";
    public static final String DEFAULT_OFFICE_PREVIEW_TYPE = "image";
    public static final String DEFAULT_FTP_USERNAME = null;
    public static final String DEFAULT_FTP_PASSWORD = null;
    public static final String DEFAULT_FTP_CONTROL_ENCODING = "UTF-8";
    public static final String DEFAULT_BASE_URL = "default";
    public static final String DEFAULT_FILE_DIR_VALUE = "default";
    public static final String DEFAULT_TRUST_HOST = "default";
    public static final String DEFAULT_PDF_DOWNLOAD_DISABLE = "true";

    public static Boolean isCacheEnabled() {
        return CACHE_ENABLED;
    }

    @Value("${cache.enabled:true}")
    public void setCacheEnabled(String cacheEnabled) {
        setCacheEnabledValueValue(Boolean.parseBoolean(cacheEnabled));
    }

    public static void setCacheEnabledValueValue(Boolean cacheEnabled) {
        CACHE_ENABLED = cacheEnabled;
    }

    public static String[] getSimText() {
        return SIM_TEXT;
    }

    @Value("${simText:txt,html,htm,asp,jsp,xml,json,properties,md,gitignore,log,java,py,c,cpp,sql,sh,bat,m,bas,prg,cmd}")
    public void setSimText(String simText) {
        String[] simTextArr = simText.split(",");
        setSimTextValue(simTextArr);
    }

    public static void setSimTextValue(String[] simText) {
        SIM_TEXT = simText;
    }

    public static String[] getMedia() {
        return MEDIA;
    }

    @Value("${media:mp3,wav,mp4,flv}")
    public void setMedia(String media) {
        String[] mediaArr = media.split(",");
        setMediaValue(mediaArr);
    }

    public static void setMediaValue(String[] Media) {
        MEDIA = Media;
    }

    public static String getOfficePreviewType() {
        return OFFICE_PREVIEW_TYPE;
    }

    @Value("${office.preview.type:image}")
    public void setOfficePreviewType(String officePreviewType) {
        setOfficePreviewTypeValue(officePreviewType);
    }

    public static void setOfficePreviewTypeValue(String officePreviewType) {
        OFFICE_PREVIEW_TYPE = officePreviewType;
    }

    public static String getFtpUsername() {
        return FTP_USERNAME;
    }

    @Value("${ftp.username:}")
    public void setFtpUsername(String ftpUsername) {
        setFtpUsernameValue(ftpUsername);
    }

    public static void setFtpUsernameValue(String ftpUsername) {
        FTP_USERNAME = ftpUsername;
    }

    public static String getFtpPassword() {
        return FTP_PASSWORD;
    }

    @Value("${ftp.password:}")
    public void setFtpPassword(String ftpPassword) {
        setFtpPasswordValue(ftpPassword);
    }

    public static void setFtpPasswordValue(String ftpPassword) {
        FTP_PASSWORD = ftpPassword;
    }

    public static String getFtpControlEncoding() {
        return FTP_CONTROL_ENCODING;
    }

    @Value("${ftp.control.encoding:UTF-8}")
    public void setFtpControlEncoding(String ftpControlEncoding) {
        setFtpControlEncodingValue(ftpControlEncoding);
    }

    public static void setFtpControlEncodingValue(String ftpControlEncoding) {
        FTP_CONTROL_ENCODING = ftpControlEncoding;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    @Value("${base.url:default}")
    public void setBaseUrl(String baseUrl) {
        setBaseUrlValue(baseUrl);
    }

    public static void setBaseUrlValue(String baseUrl) {
        BASE_URL = baseUrl;
    }

    public static String getFileDir() {
        return FILE_DIR;
    }

    @Value("${file.dir:default}")
    public void setFileDir(String fileDir) {
        setFileDirValue(fileDir);
    }

    public static void setFileDirValue(String fileDir) {
        if (!DEFAULT_FILE_DIR_VALUE.equals(fileDir.toLowerCase())) {
            if (!fileDir.endsWith(File.separator)) {
                fileDir = fileDir + File.separator;
            }
            FILE_DIR = fileDir;
        }
    }

    @Value("${trust.host:default}")
    public void setTrustHost(String trustHost) {
        setTrustHostValue(trustHost);
    }

    public static void setTrustHostValue(String trustHost) {
        CopyOnWriteArraySet<String> trustHostSet;
        if (DEFAULT_TRUST_HOST.equals(trustHost.toLowerCase())) {
            trustHostSet = new CopyOnWriteArraySet<>();
        } else {
            String[] trustHostArray = trustHost.toLowerCase().split(",");
            trustHostSet = new CopyOnWriteArraySet<>(Arrays.asList(trustHostArray));
            setTrustHostSet(trustHostSet);
        }
        setTrustHostSet(trustHostSet);
    }

    public static Set<String> getTrustHostSet() {
        return TRUST_HOST_SET;
    }

    private static void setTrustHostSet(CopyOnWriteArraySet<String> trustHostSet) {
        ConfigConstants.TRUST_HOST_SET = trustHostSet;
    }

    public static String getPdfDownloadDisable() {
        return PDF_DOWNLOAD_DISABLE;
    }


    @Value("${pdf.download.disable:true}")
    public void setPdfDownloadDisable(String pdfDownloadDisable) {
        setPdfDownloadDisableValue(pdfDownloadDisable);
    }
    public static void setPdfDownloadDisableValue(String pdfDownloadDisable) {
        PDF_DOWNLOAD_DISABLE = pdfDownloadDisable;
    }

}
