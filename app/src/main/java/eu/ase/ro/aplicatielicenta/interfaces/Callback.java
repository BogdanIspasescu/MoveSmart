package eu.ase.ro.aplicatielicenta.interfaces;

public interface Callback<R> {

    void runResultOnUiThread(R result);
}

