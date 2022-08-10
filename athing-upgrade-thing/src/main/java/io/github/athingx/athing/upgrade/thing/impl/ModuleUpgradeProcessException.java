package io.github.athingx.athing.upgrade.thing.impl;

/**
 * 模块升级异常
 */
class ModuleUpgradeProcessException extends Exception {

    private final String moduleId;
    private final int step;
    private final String desc;

    public ModuleUpgradeProcessException(String moduleId, int step, String desc) {
        super("module: %s upgrade failure, step=%s;desc=%s".formatted(moduleId, step, desc));
        this.moduleId = moduleId;
        this.step = step;
        this.desc = desc;
    }

    public ModuleUpgradeProcessException(String moduleId, int step, Throwable cause) {
        super("module: %s upgrade failure, step=%s;desc=%s;".formatted(moduleId, step, cause.getMessage()), cause);
        this.moduleId = moduleId;
        this.step = step;
        this.desc = cause.getMessage();
    }

    public String getModuleId() {
        return moduleId;
    }

    public int getStep() {
        return step;
    }

    public String getDesc() {
        return desc;
    }

}
