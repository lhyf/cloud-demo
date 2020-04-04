package org.lhyf.cloud.entity;

import lombok.Data;

import java.io.Serializable;

/****
 * @author YF
 * @date 2020-03-22 12:53
 * @desc Payment
 *
 **/
@Data
public class Payment implements Serializable {
    private Long id;
    private String serial;
}
