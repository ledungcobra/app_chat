package com.appchat.server.dto;


import lombok.Data;

import java.io.Serializable;
import java.util.concurrent.Future;

@Data
public class CommandObject implements Serializable {
    protected Command command;
    protected Object payload;
    private Future<?> result;


}
