import Foundation
import netflowSample

@MainActor
final class TodoListViewModel: ObservableObject {
    private let viewModel = DependencyHelper.shared.mainViewModel

    @Published private(set) var todos: [Todo] = []
    @Published private(set) var isLoading: Bool = false
    @Published private(set) var hasNextPage: Bool = false
    @Published private(set) var todoState: TodoState = TodoState(
        title: "Add Todo",
        todoTitle: "",
        buttonTitle: "Add",
        isChecked: false,
        isAddUpdateDialogVisible: false
    )

    private let delegate = PagingCollectionViewController<Todo>()

    init() {
        observeContentsLoaded()
        observeDataChanged()
        observeLoadStates()
        observeTodoState()
    }

    // MARK: - Actions

    func showAddDialog() {
        viewModel.onAction(action: TodoActionShowAddUpdateDialog(todo: nil))
    }

    func showEditDialog(todo: Todo) {
        viewModel.onAction(action: TodoActionShowAddUpdateDialog(todo: todo))
    }

    func dismissDialog() {
        viewModel.onAction(action: TodoActionDismissAddUpdateDialog())
    }

    func updateTitle(_ title: String) {
        viewModel.onAction(action: TodoActionUpdateTitle(title: title))
    }

    func updateIsChecked(_ isChecked: Bool) {
        viewModel.onAction(action: TodoActionUpdateIsChecked(isChecked: isChecked))
    }

    func submit(id: Int32?) {
        viewModel.onAction(action: TodoActionAddUpdateTodo(id: id.map { KotlinInt(int: $0) }))
    }

    func toggleTodoCheck(todo: Todo) {
        viewModel.onAction(action: TodoActionUpdateTodoCheck(todo: todo))
    }

    func deleteTodo(id: Int32) {
        viewModel.onAction(action: TodoActionDeleteTodo(id: id))
    }

    func loadNextPage() {
        delegate.loadNextPage()
    }

    // MARK: - Private observers

    private func observeContentsLoaded() {
        Task {
            for await pagingData in viewModel.todos {
                delegate.submitData(pagingData: pagingData)
            }
        }
    }

    private func observeDataChanged() {
        Task {
            for await _ in delegate.onPagesUpdatedFlow {
                self.todos = delegate.getItems()
                self.isLoading = false
            }
        }
    }

    private func observeLoadStates() {
            Task {
                for await loadState in delegate.loadStateFlow {
                    switch loadState?.append {
                    case let notLoading as Paging_commonLoadState.NotLoading:
                        self.hasNextPage = !notLoading.endOfPaginationReached
                    default:
                        break
                    }
                    
                    switch loadState?.refresh {
                    case _ as Paging_commonLoadState.Loading:
                        self.isLoading = true
                    default:
                        break
                    }
                }
            }
        }

    private func observeTodoState() {
        Task {
            for await state in viewModel.state {
                self.todoState = state
            }
        }
    }

    deinit {
        delegate.clearScope()
    }
}
