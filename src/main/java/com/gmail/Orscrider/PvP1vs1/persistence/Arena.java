/*
 * Decompiled with CFR 0.152.
 */
package com.gmail.Orscrider.PvP1vs1.persistence;

public class Arena {
    private Integer arenaid;
    private String name;

    public Arena(Integer arenaid, String name) {
        this.arenaid = arenaid;
        this.name = name;
    }

    public Integer getArenaid() {
        return this.arenaid;
    }

    public void setArenaid(Integer arenaid) {
        this.arenaid = arenaid;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

