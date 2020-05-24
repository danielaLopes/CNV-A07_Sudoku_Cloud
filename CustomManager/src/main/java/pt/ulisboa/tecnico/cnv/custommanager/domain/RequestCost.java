package pt.ulisboa.tecnico.cnv.custommanager.domain;

public class RequestCost {

  private Long fieldLoads;
  private int cpu;
  private int time;

  public RequestCost(Long fieldLoads) {

      this.fieldLoads = fieldLoads;
      // TODO: calculate cpu from fieldLoads
      // TODO: calculate time from fieldLoads
  }

  public void setFieldLoads(Long fieldLoads) {
      this.fieldLoads = fieldLoads;
  }

  public Long getFieldLoads() {
      return this.fieldLoads;
  }

    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    public int getCpu() {
        return this.cpu;
    }

    public int getTime() { return this.time; }
}