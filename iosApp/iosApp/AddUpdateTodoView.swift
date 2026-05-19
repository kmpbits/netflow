import SwiftUI

struct AddUpdateTodoView: View {
    let title: String
    let buttonTitle: String
    @Binding var todoTitle: String
    @Binding var isChecked: Bool
    let onDismiss: () -> Void
    let onSubmit: () -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Enter title", text: $todoTitle)
                }

                Section {
                    Toggle("Checked?", isOn: $isChecked)
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onDismiss)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(buttonTitle, action: onSubmit)
                        .disabled(todoTitle.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
        .presentationDetents([.medium])
    }
}
