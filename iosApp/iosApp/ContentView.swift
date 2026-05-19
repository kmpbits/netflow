import SwiftUI
import netflowSample

struct ContentView: View {
    @StateObject private var viewModel = TodoListViewModel()
    @State private var editingTodoId: Int32? = nil
    @State private var todoTitleInput: String = ""
    @State private var isCheckedInput: Bool = false

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading && viewModel.todos.isEmpty {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        ForEach(viewModel.todos, id: \.id) { todo in
                            TodoItemView(
                                todo: todo,
                                onCheckChanged: { _ in
                                    viewModel.toggleTodoCheck(todo: todo)
                                },
                                onDelete: {
                                    viewModel.deleteTodo(id: todo.id)
                                }
                            )
                            .onTapGesture {
                                editingTodoId = todo.id
                                viewModel.showEditDialog(todo: todo)
                            }
                            .onAppear {
                                if todo.id == viewModel.todos.last?.id {
                                    viewModel.loadNextPage()
                                }
                            }
                        }

                        if viewModel.isLoading {
                            HStack {
                                Spacer()
                                ProgressView()
                                Spacer()
                            }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Todo List")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        editingTodoId = nil
                        viewModel.showAddDialog()
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: Binding(
                get: { viewModel.todoState.isAddUpdateDialogVisible },
                set: { if !$0 { viewModel.dismissDialog() } }
            )) {
                AddUpdateTodoView(
                    title: viewModel.todoState.title,
                    buttonTitle: viewModel.todoState.buttonTitle,
                    todoTitle: Binding(
                        get: { viewModel.todoState.todoTitle },
                        set: { viewModel.updateTitle($0) }
                    ),
                    isChecked: Binding(
                        get: { viewModel.todoState.isChecked },
                        set: { viewModel.updateIsChecked($0) }
                    ),
                    onDismiss: { viewModel.dismissDialog() },
                    onSubmit: { viewModel.submit(id: editingTodoId) }
                )
            }
        }
    }
}

#Preview {
    ContentView()
}
