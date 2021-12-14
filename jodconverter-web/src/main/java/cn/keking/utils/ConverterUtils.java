package cn.keking.utils;

import com.sun.star.document.UpdateDocMode;
import cn.keking.extend.ControlDocumentFormatRegistry;
import org.apache.commons.lang3.StringUtils;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.artofsolving.jodconverter.office.OfficeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 文件类型转换 工具类
 *
 * @author yudian-it
 * @date 2017/11/13
 */
@Component
public class ConverterUtils {

    private final Logger logger = LoggerFactory.getLogger(ConverterUtils.class);

    // office任务管理器
    private OfficeManager officeManager;

    /**
     * spring容器启动后初始化office组件
     */
    @PostConstruct
    public void initOfficeManager() {
        File officeHome;
        // 获取文字处理软件OpenOffice或LiberOffice的路径
        // 一定要先安装环境,并且在application.properties将软件路径配置正确,否则就会报下面"找不到office组件"错误
        // TODO: 2021/6/3 可以增加多个openoffice 来提高转换效率
        officeHome = OfficeUtils.getDefaultOfficeHome();
        if (officeHome == null) {
            throw new RuntimeException("找不到office组件，请确认'office.home'配置是否有误");
        }
        // 先结束office进程
        boolean killOffice = killProcess();
        if (killOffice) {
            logger.warn("检测到有正在运行的office进程，已自动结束该进程");
        }
        try {
            // office管理器配置
            DefaultOfficeManagerConfiguration configuration = new DefaultOfficeManagerConfiguration();
            configuration.setOfficeHome(officeHome);
            configuration.setPortNumber(8100);
            // 设置任务执行超时为5分钟
            configuration.setTaskExecutionTimeout(1000 * 60 * 5L);
            // 设置任务队列超时为24小时
            configuration.setTaskQueueTimeout(1000 * 60 * 60 * 24L);
            // 构建Office管理器(包含多个)
            officeManager = configuration.buildOfficeManager();
            // 启动office管理器
            officeManager.start();
        } catch (Exception e) {
            logger.error("启动office组件失败，请检查office组件是否可用");
            throw e;
        }
    }

    /**
     * 获取 office 类型转换器
     * @return
     */
    public OfficeDocumentConverter getDocumentConverter() {
        OfficeDocumentConverter converter = new OfficeDocumentConverter(officeManager, new ControlDocumentFormatRegistry());
        converter.setDefaultLoadProperties(getLoadProperties());
        return converter;
    }

    private Map<String,?> getLoadProperties() {
        Map<String,Object> loadProperties = new HashMap<>(10);
        loadProperties.put("Hidden", true);
        loadProperties.put("ReadOnly", true);
        loadProperties.put("UpdateDocMode", UpdateDocMode.QUIET_UPDATE);
        loadProperties.put("CharacterSet", StandardCharsets.UTF_8.name());
        return loadProperties;
    }

    /**
     * 杀死进程
     * @return
     */
    private boolean killProcess() {
        boolean flag = false;
        Properties props = System.getProperties();
        try {
            // windows系统
            if (props.getProperty("os.name").toLowerCase().contains("windows")) {
                Process p = Runtime.getRuntime().exec("cmd /c tasklist ");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream os = p.getInputStream();
                byte[] b = new byte[256];
                while (os.read(b) > 0) {
                    baos.write(b);
                }
                String s = baos.toString();
                if (s.contains("soffice.bin")) {
                    Runtime.getRuntime().exec("taskkill /im " + "soffice.bin" + " /f");
                    flag = true;
                }
            } else {
                Process p = Runtime.getRuntime().exec(new String[]{"sh","-c","ps -ef | grep " + "soffice.bin"});
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream os = p.getInputStream();
                byte[] b = new byte[256];
                while (os.read(b) > 0) {
                    baos.write(b);
                }
                String s = baos.toString();
                if (StringUtils.ordinalIndexOf(s, "soffice.bin", 3) > 0) {
                    String[] cmd ={"sh","-c","kill -15 `ps -ef|grep " + "soffice.bin" + "|awk 'NR==1{print $2}'`"};
                    Runtime.getRuntime().exec(cmd);
                    flag = true;
                }
            }
        } catch (IOException e) {
            logger.error("检测office进程异常", e);
        }
        return flag;
    }

    /**
     * spring容器关闭时,将软件也关闭
     */
    @PreDestroy
    public void destroyOfficeManager(){
        if (null != officeManager && officeManager.isRunning()) {
            officeManager.stop();
        }
    }

}
