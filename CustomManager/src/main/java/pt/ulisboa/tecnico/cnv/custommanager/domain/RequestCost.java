package pt.ulisboa.tecnico.cnv.custommanager.domain;

public class RequestCost {

    private Long fieldLoads;

    public RequestCost(Long fieldLoads) {
        this.fieldLoads = fieldLoads;
    }

    public void setFieldLoads(Long fieldLoads) {
        this.fieldLoads = fieldLoads;
    }

    public Long getFieldLoads() {
        return fieldLoads;
    }

}