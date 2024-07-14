package com.example.roomapplication

data class ContactState(val firstName:String = "",val lastName:String  = "", val sortType:SortType = SortType.FIRST_NAME,val phoneNumber:String = "",val isAddingContact:Boolean = false,val contact:List<Contact> = emptyList())