package pt.ulisboa.tecnico.cnv.custommanager.domain;

public class RequestCost {

    public int _cost;

    public  RequestCost(int cost) {
        _cost = cost;
    }

    public int getCost() {
        return _cost;
    }
}