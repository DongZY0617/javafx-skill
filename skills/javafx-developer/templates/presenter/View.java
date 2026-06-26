package {{packageName}}.view;

import java.util.List;

import {{packageName}}.model.{{entityName}};

/**
 * View 接口模板（MVP 模式）。
 * <p>
 * 抽象 View 暴露给 Presenter 的能力，不含任何 JavaFX 依赖。
 * Controller 实现此接口，将 UI 操作封装为接口方法，
 * Presenter 通过接口控制 View，实现 UI 与逻辑的完全解耦。
 * </p>
 * <p>
 * 设计原则：接口方法应尽量细粒度，只暴露 Presenter 需要的操作，
 * 避免将整个 Stage 或 Scene 传入 Presenter。
 * </p>
 */
public interface {{entityName}}View {

    /**
     * 获取用户输入的数据。
     *
     * @return 包含输入数据的实体对象；若输入无效返回 null
     */
    {{entityName}} getInputData();

    /**
     * 刷新数据列表显示。
     *
     * @param list 待显示的数据列表
     */
    void setDataList(List<{{entityName}}> list);

    /**
     * 清空输入区域。
     */
    void clearInput();

    /**
     * 显示校验错误提示。
     *
     * @param message 错误描述
     */
    void showValidationError(String message);
}
