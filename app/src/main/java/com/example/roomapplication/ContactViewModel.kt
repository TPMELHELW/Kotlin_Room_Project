package com.example.roomapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ContactViewModel(
    private val dao:ContactDao
): ViewModel() {

    private val _state = MutableStateFlow(ContactState())
    private val _typeSort = MutableStateFlow(SortType.FIRST_NAME)
    private val _contacts = _typeSort.flatMapLatest { sortType-> when(sortType){
        SortType.FIRST_NAME -> dao.getContactByFirstName()
        SortType.LAST_NAME -> dao.getContactByLastName()
        SortType.PHONE_NUMBER -> dao.getContactByPhoneNumber()
    } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val state = combine(_state,_typeSort,_contacts){state,typeSort,contacts->
        state.copy(contact = contacts, sortType = typeSort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContactState())

fun onEvent(event:ContactEvent){
    when(event){
        is ContactEvent.DeleteContact -> {
viewModelScope.launch {
    dao.deleteContact(event.contact)

}
        }
        ContactEvent.HideDialog -> _state.update {it.copy(
            isAddingContact = false
        )}
        ContactEvent.SaveContact -> {
            val firstName = state.value.firstName
            val lastName = state.value.lastName
            val phoneNumber = state.value.phoneNumber

            if(firstName.isBlank() || lastName.isBlank() || phoneNumber.isBlank())return

            val contact:Contact  = Contact(firstName = firstName, lastName = lastName, phoneNumber = phoneNumber)

            viewModelScope.launch {
                dao.upsertContact(contact)
            }

            _state.update {
                it.copy(
                    firstName = "",
                    lastName = "",
                    phoneNumber = "",
                    isAddingContact = false)
            }
        }
        is ContactEvent.SetFirstName -> {
            _state.update{it.copy(firstName =  event.firstName)}
        }
        is ContactEvent.SetLastName -> {
            _state.update{it.copy(lastName =  event.lastName)}
        }
        is ContactEvent.SetPhoneNumber -> {
            _state.update{it.copy(phoneNumber =  event.phoneNumber)}
        }
        ContactEvent.ShowDialog ->_state.update{it.copy(isAddingContact = true)}
        is ContactEvent.SortContact -> {
            _typeSort.value = event.sortType
        }
    }
}
}