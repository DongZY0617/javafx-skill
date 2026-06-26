package {{packageName}}.presenter;

import java.util.List;

import {{packageName}}.model.{{entityName}};
import {{packageName}}.service.{{entityName}}Service;
import {{packageName}}.view.{{entityName}}View;

/**
 * Presenter 模板（MVP 模式）。
 * <p>
 * 持有 {@link {{entityName}}View} 接口引用，通过接口显式控制 UI，
 * 不依赖任何 JavaFX 控件，可独立进行单元测试。
 * </p>
 * <p>
 * Controller 实现 View 接口后，将自身引用传入 Presenter 构造器，
 * 所有业务逻辑由 Presenter 处理，Controller 仅负责 UI 操作的胶水代码。
 * </p>
 */
public class {{entityName}}Presenter {

    private final {{entityName}}View view;
    private final {{entityName}}Service service;

    /**
     * 构造 Presenter。
     *
     * @param view    View 接口引用（通常由 Controller 实现）
     * @param service 业务逻辑层
     */
    public {{entityName}}Presenter({{entityName}}View view, {{entityName}}Service service) {
        this.view = view;
        this.service = service;
    }

    /**
     * 加载全部数据并更新视图。
     */
    public void onLoadData() {
        List<{{entityName}}> list = service.findAll();
        view.setDataList(list);
    }

    /**
     * 处理新增操作。
     * <p>
     * 从视图获取输入数据，校验后调用 Service 保存，并刷新列表。
     * </p>
     */
    public void onAdd() {
        {{entityName}} entity = view.getInputData();
        if (entity == null || !isValid(entity)) {
            view.showValidationError("数据不完整或格式错误");
            return;
        }
        service.save(entity);
        view.clearInput();
        onLoadData();
    }

    /**
     * 处理删除操作。
     *
     * @param id 待删除记录的主键
     */
    public void onDelete(Long id) {
        service.deleteById(id);
        onLoadData();
    }

    /**
     * 简单的数据校验，可按需扩展。
     *
     * @param entity 待校验实体
     * @return 校验通过返回 true
     */
    private boolean isValid({{entityName}} entity) {
        return entity != null;
    }
}
