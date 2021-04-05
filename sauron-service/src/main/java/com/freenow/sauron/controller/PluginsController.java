package com.freenow.sauron.controller;

import com.freenow.sauron.plugins.AutomaticPluginUpdater;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/v1/plugins")
public class PluginsController
{
    private final AutomaticPluginUpdater automaticPluginUpdater;

    private final PluginManager pluginManager;


    @Autowired
    public PluginsController(AutomaticPluginUpdater automaticPluginUpdater, PluginManager pluginManager)
    {
        this.automaticPluginUpdater = automaticPluginUpdater;
        this.pluginManager = pluginManager;
    }


    @GetMapping
    @ApiOperation(value = "Return all loaded plugins.")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Return a list of all loaded plugins", responseContainer = "List", response = PluginDescriptor.class),
        @ApiResponse(code = 500, message = "Internal Server Error.")
    })
    public ResponseEntity<List<PluginDescriptor>> list()
    {
        return ResponseEntity.ok(pluginManager.getPlugins().stream().map(PluginWrapper::getDescriptor).collect(Collectors.toList()));
    }


    @GetMapping("/ids")
    @ApiOperation(value = "Return all loaded plugins id.")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Return a list of all loaded plugins id", responseContainer = "List", response = String.class),
        @ApiResponse(code = 500, message = "Internal Server Error.")
    })
    public ResponseEntity<List<String>> listPluginsId()
    {
        return ResponseEntity.ok(pluginManager.getPlugins().stream().map(PluginWrapper::getDescriptor).map(PluginDescriptor::getPluginId).collect(Collectors.toList()));
    }


    @PutMapping("/reload")
    @ApiOperation(value = "Request a plugin synchronous reloading process.")
    @ApiResponses({
        @ApiResponse(code = 200, message = "New reloading process has been started"),
        @ApiResponse(code = 500, message = "Internal Server Error.")
    })
    public ResponseEntity<Void> reload()
    {
        automaticPluginUpdater.update();
        return ResponseEntity.ok().build();
    }


    @PutMapping("/forcereload/{pluginId}")
    @ApiOperation(value = "Force a plugin by id to be reloaded.")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Plugin has been reloaded"),
        @ApiResponse(code = 500, message = "Internal Server Error.")
    })
    public ResponseEntity<Void> forceReload(@PathVariable String pluginId)
    {
        automaticPluginUpdater.forceReload(pluginId);
        return ResponseEntity.ok().build();
    }
}