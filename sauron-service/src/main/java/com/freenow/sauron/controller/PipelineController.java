package com.freenow.sauron.controller;

import com.freenow.sauron.datatransferobject.BuildRequest;
import com.freenow.sauron.service.PipelineService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PipelineController
{
    private final PipelineService pipelineService;


    @Autowired
    public PipelineController(PipelineService pipelineService)
    {
        this.pipelineService = pipelineService;
    }


    @PostMapping("/build")
    @ApiOperation(value = "Start a new asynchronous build in Sauron.")
    @ApiResponses({
        @ApiResponse(code = 200, message = "New build has been accepted"),
        @ApiResponse(code = 400, message = "Bad request. Invalid input parameters."),
        @ApiResponse(code = 500, message = "Internal Server Error.")
    })
    public ResponseEntity<Void> build(@Valid @RequestBody BuildRequest request)
    {
        pipelineService.publish(request);
        return ResponseEntity.ok().build();
    }
}
