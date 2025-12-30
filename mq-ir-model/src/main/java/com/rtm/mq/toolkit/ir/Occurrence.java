package com.rtm.mq.toolkit.ir;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Min/max occurrence information for a repeating segment.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Occurrence {
    private int minOccurs;
    private Integer maxOccurs;

    public Occurrence() {
    }

    public Occurrence(int minOccurs, Integer maxOccurs) {
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }

    public Integer getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(Integer maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public boolean isSingle() {
        return maxOccurs != null && maxOccurs == 1 && minOccurs == 1;
    }

    public boolean isRepeating() {
        return maxOccurs == null || maxOccurs > 1 || minOccurs > 1;
    }
}
