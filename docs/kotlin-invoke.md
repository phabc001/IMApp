在 Kotlin 中，**`invoke`** 是用来调用一个函数类型变量的方法。对于高阶函数变量，`invoke` 的作用是显式地调用存储在这个变量中的函数。

---

### 1. **`invoke` 是什么？**
`invoke` 是函数类型变量的内置调用操作符，可以通过以下两种方式调用：
- **显式调用：** 使用 `.invoke()` 调用
- **简写：** 直接用 `()` 调用（推荐这种方式）

例如：
```kotlin
val sayHello: (String) -> Unit = { name -> println("Hello, $name!") }
sayHello.invoke("Alice") // 显式调用
sayHello("Alice")        // 简写，效果相同
```

以上两种调用方式完全等价。

---

### 2. **为什么要用 `?.invoke`？**
`?.invoke` 是为了处理高阶函数变量的 **可空性（nullable）**。

在你的代码中：
```kotlin
onMessageReceived?.invoke(message)
```
含义是：
- 如果 `onMessageReceived` 不是 `null`，调用它并传入参数 `message`。
- 如果 `onMessageReceived` 是 `null`，则什么都不做。

这是一种常见的安全调用操作，防止空指针异常（NullPointerException）。

### 3. **等价的代码**
上述代码等价于：
```kotlin
if (onMessageReceived != null) {
    onMessageReceived.invoke(message)
}
```
但 Kotlin 的 `?.invoke` 语法更简洁，更符合惯用风格。

---

### 4. **完整调用流程**
在 WebSocketManager 中，当接收到消息时，代码会这样调用：
```kotlin
onMessageReceived?.invoke(message)
```
具体含义：
1. WebSocket 收到消息后，将消息封装为 `message`。
2. 检查 `onMessageReceived` 是否为 `null`：
    - 如果不是 `null`，调用存储的回调函数，并传递 `message`。
    - 如果是 `null`，直接跳过，不会有任何操作。
3. 回调函数会处理 `message`，例如更新 UI 或打印日志。

---

### 5. **为什么不直接用 `onMessageReceived(message)`？**
因为 `onMessageReceived` 是可空的（类型是 `((String) -> Unit)?`），可能为 `null`。如果直接调用而不检查：
```kotlin
onMessageReceived(message)
```
在 `onMessageReceived` 为 `null` 时会抛出 **NullPointerException**。

通过 `?.invoke`，Kotlin 提供了一种安全的方式来调用可空函数。

---

### 6. **总结**
`onMessageReceived?.invoke(message)` 的作用：
- 检查回调是否存在（不为 `null`）。
- 如果存在，调用回调函数并将 `message` 作为参数传入。
- 如果回调是 `null`，避免空指针异常，不进行任何操作。

这是一种优雅且安全的写法，适合处理高阶函数的调用场景。