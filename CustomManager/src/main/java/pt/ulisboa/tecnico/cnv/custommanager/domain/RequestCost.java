package pt.ulisboa.tecnico.cnv.custommanager.domain;

public class RequestCost {

    private Long fieldLoads;
    private Double cpuPercentage;
    private Integer timeEstimation;

    public RequestCost(Long fieldLoads) {
        this.fieldLoads = fieldLoads;
    }

    public RequestCost(Long fieldLoads, Double cpuPercentage) {
        this.fieldLoads = fieldLoads;
        this.cpuPercentage = cpuPercentage;
    }

    public RequestCost(Long fieldLoads, Double cpuPercentage, Integer timeEstimation) {
        this.fieldLoads = fieldLoads;
        this.cpuPercentage = cpuPercentage;
        this.timeEstimation = timeEstimation;
    }

    public void setFieldLoads(Long fieldLoads) {
      this.fieldLoads = fieldLoads;
  }

    public Long getFieldLoads() {
      return this.fieldLoads;
  }

    public void setCpuPercentage(Double cpu) {
        this.cpuPercentage = cpu;
    }

    public Double getCpuPercentage() {
        return this.cpuPercentage;
    }

    public Integer getTimeEstimation() { return this.timeEstimation; }

    @Override
    public String toString() {
        String s = "fieldLoads: " + fieldLoads;
        s += "; cpuPercentage: " + cpuPercentage;
        s+= "; timeEstimation: " + timeEstimation;
        return s;
    }
}