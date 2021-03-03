package cn.keking.model;

/**
 * 文件属性类
 * Created by kl on 2018/1/17.
 * Content :
 */
public class FileAttribute {

    private FileType type;  // 文件类型,每个文件类型都有对应的转换Service 例如: office -> officeFilePreviewImpl

    private String suffix;  // 文件后缀名 例如: xlsx

    private String name;    // 原文件名称 例如: 1594862350221.xlsx

    private String url;     //  远程端文件下载路径 例如: http://47.111.75.34:8885/viewFile/programes/jdxm/excelDemo/1594862350221.xlsx

    public FileAttribute() {
    }

    public FileAttribute(FileType type, String suffix, String name, String url) {
        this.type = type;
        this.suffix = suffix;
        this.name = name;
        this.url = url;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
