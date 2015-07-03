package org.fxpart.mockserver;

/**
 * Created by metairie on 22-Jun-15.
 */
public class ProfessionBean {

    private long id;
    private String code;
    private String name;

    public ProfessionBean(){

    }

    public ProfessionBean(long lid, String scode, String sname) {
        id = lid;
        code = scode;
        name = sname;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
