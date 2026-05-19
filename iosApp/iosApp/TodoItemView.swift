import SwiftUI
import netflowSample

struct TodoItemView: View {
    let todo: Todo
    let onCheckChanged: (Bool) -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack {
            Text(todo.title)
                .frame(maxWidth: .infinity, alignment: .leading)

            Toggle("", isOn: Binding(
                get: { todo.completed },
                set: { onCheckChanged($0) }
            ))
            .labelsHidden()
            .frame(width: 50)

            Button(role: .destructive, action: onDelete) {
                Image(systemName: "trash")
                    .foregroundColor(.red)
            }
            .buttonStyle(.borderless)
        }
        .contentShape(Rectangle())
    }
}
