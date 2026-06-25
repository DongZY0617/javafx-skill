package {{packageName}}.viewmodel;

import javafx.beans.property.*;
import {{packageName}}.model.ObservableModel;

/**
 * ViewModel 模板（MVVM 模式）。
 * 暴露 Properties 供 View 绑定，封装业务逻辑。
 */
public class UserViewModel {

    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final BooleanProperty saveDisabled = new SimpleBooleanProperty(true);

    public UserViewModel() {
        // 保存按钮在名称为空时禁用
        saveDisabled.bind(name.isEmpty());
    }

    // 名称属性
    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }

    // 描述属性
    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String value) { description.set(value); }

    // 保存按钮禁用状态（只读）
    public BooleanProperty saveDisabledProperty() { return saveDisabled; }

    /** 保存操作（委托给 Service 层） */
    public void save() {
        // TODO: 委托给 Service 层执行持久化
        System.out.println("保存: " + getName());
    }

    /** 从模型加载数据 */
    public void loadFromModel(ObservableModel model) {
        if (model != null) {
            // TODO: 从模型同步数据到 ViewModel
            // 可用字段：model.getName() / model.getCreatedAt()
        }
    }
}
