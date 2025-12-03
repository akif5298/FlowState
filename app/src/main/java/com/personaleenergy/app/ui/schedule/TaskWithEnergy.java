package com.personaleenergy.app.ui.schedule;

import com.flowstate.app.data.models.EnergyLevel;

/**
 * Data class for a task with its energy requirement
 */
public class TaskWithEnergy {
    private String taskName;
    private EnergyLevel energyLevel;
    
    public TaskWithEnergy(String taskName, EnergyLevel energyLevel) {
        this.taskName = taskName;
        this.energyLevel = energyLevel;
    }
    
    public String getTaskName() {
        return taskName;
    }
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    public EnergyLevel getEnergyLevel() {
        return energyLevel;
    }
    
    public void setEnergyLevel(EnergyLevel energyLevel) {
        this.energyLevel = energyLevel;
    }
}

