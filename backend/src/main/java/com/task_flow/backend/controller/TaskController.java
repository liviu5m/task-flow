package com.task_flow.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @RequestMapping("/create")
    public String create() {
        return "create";
    }

    @RequestMapping("/update")
    public String update() {
        return "update";
    }

    @RequestMapping("/delete")
    public String delete() {
        return "delete";
    }

}
