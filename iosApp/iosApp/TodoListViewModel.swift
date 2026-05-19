//
//  TodoListViewModel.swift
//  iosApp
//
//  Created by Joel Caetano on 19/05/2026.
//

import Foundation
import netflowSample

@MainActor
final class TodoListViewModel: ObservableObject {
    private let viewModel = DependencyHelper.shared.mainViewModel
    
    @Published private(set) var todos: [Todo] = []
    @Published private(set) var isLoading: Bool = false
    @Published private(set) var error: String? = nil
    @Published private(set) var hasNextPage: Bool = false
    @Published private(set) var todoState: TodoState = TodoState(title: "Add Todo", todoTitle: "", buttonTitle: "Add", isChecked: false, isAddUpdateDialogVisible: false)
    
    private let delegate = PagingCollectionViewController<Todo>()
    
    func loadNextPage() {
        delegate.loadNextPage()
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
    
    private func observeDataChanged() {
            Task {
                for await _ in delegate.onPagesUpdatedFlow {
                    let items = delegate.getItems()
                    self.todos = items
                    self.isLoading = false
                }
            }
        }
        
        private func observeContentsLoaded() {
            Task {
                for await pagingData in viewModel.todos {
                    delegate.submitData(pagingData: pagingData)
                }
            }
        }
    
    private func observeTodoState() {
        Task {
            for await todoState in viewModel.state {
                
            }
        }
    }
    
    deinit {
        delegate.clearScope()
    }
}
