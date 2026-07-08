package org.dromara.autotable.springboot;

/**
 * 需要提前初始化的 Bean 标记接口。
 * <p>
 * <b>背景</b>：{@link AutoTableAutoConfig} 在构造器中完成全局配置注入并调用 {@code AutoTableBootstrap.start()} 启动建表流程。
 * 此时通过 {@code ObjectProvider} 获取的 adapter、scanner 等 Bean 必须已经创建完毕，
 * 否则 {@code ifAvailable()} 会因为 Bean 尚未注册而返回空，导致使用默认实现。
 * <p>
 * <b>机制</b>：{@link AutoTableAutoConfig} 构造器的<b>第一个参数</b>是 {@code ObjectProvider<InitializeBeans>}，
 * 并在构造器开头通过 {@code initializeBeans.orderedStream()} 强制触发所有实现类的实例化。
 * 由于 Spring 的依赖注入链，实现类所依赖的其他 Bean（如 adapter config）也会被一并提前创建。
 * <p>
 * <b>使用方式</b>：adapter 模块的核心实现类（如 {@code MybatisPlusExtendedMetadataAdapter}）
 * 只需实现此空接口，即可确保在 {@code AutoTableAutoConfig} 处理后续的
 * {@code ObjectProvider<AutoTableMetadataAdapter>} 等参数之前被创建并注册到 Spring 容器。
 * <p>
 * <b>示例</b>：
 * <pre>{@code
 * // MP 适配器扩展类，通过实现 InitializeBeans 确保提前初始化
 * public class MybatisPlusExtendedMetadataAdapter
 *         extends MybatisPlusMetadataAdapter
 *         implements InitializeBeans {
 *     // ...
 * }
 * }</pre>
 *
 * @see AutoTableAutoConfig
 * @author don
 */
public interface InitializeBeans {
}
