package com.example.canteen.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.canteen.R
import com.example.canteen.activities.MainActivity
import com.example.canteen.adapters.ChatAdapter
import com.example.canteen.databinding.FragmentChatBinding
import com.example.canteen.models.Chat
import com.example.canteen.models.User
import com.example.canteen.utilities.*
import com.example.canteen.viewmodels.ChatViewModel
import com.google.gson.Gson
import java.util.*


class ChatFragment : Fragment(){

    private lateinit var binding: FragmentChatBinding
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var chatMessages: MutableList<Chat>
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var conversionId: String
    private lateinit var receiverUser: User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_chat, container, false
        )
        loadReceiverDetails()
        setObservers()
        setListeners()
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
        preferenceManager.getString(Constants.KEY_USER_ID)?.let {
            chatViewModel.getMessagesById(it, receiverUser.id)//获取聊天记录
        }

    }

    private fun init() {
        preferenceManager = requireActivity().getPreferenceManager()
        chatMessages = ArrayList()
        chatAdapter = ChatAdapter(
            chatMessages,
            receiverUser.image.toBitmap(),
            preferenceManager.getString(Constants.KEY_USER_ID)!!
        )
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun loadReceiverDetails() {
        receiverUser = arguments?.getParcelable<User>("KEY_USER") as User
        binding.textName.text = receiverUser.name
    }

    private fun setObservers() {
        with(chatViewModel) {
            chatListLiveData.observe(viewLifecycleOwner) { list ->
                val it = list as MutableList
                it.sortBy { it.dateTime }
                chatMessages.addAll(it)
                chatAdapter.notifyDataSetChanged() //刷新数据
                binding.chatRecyclerView.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }
        }
        (requireActivity() as MainActivity).socketService.chatMessage.observe(viewLifecycleOwner){
            chatMessages.add(it)
            chatAdapter.notifyItemRangeInserted(chatMessages.size, chatMessages.size) //插入新的的数据
            binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1) //设置滚动到末尾的位置

        }

    }

    private fun setListeners() {
        binding.imageBack.setOnClickListener { v -> requireActivity().onBackPressed() }
        binding.layoutSent.setOnClickListener { v -> sendMessage() }
    }

    private fun listenMessage() {}

    private fun sendMessage() {
        val message = Chat(
            receiverId = receiverUser.id,
            senderId = requireActivity().getPreferenceManager().getString(Constants.KEY_USER_ID)!!,
            message = binding.inputMessage.text.toString(),
            dateTime = Date()
        )
        (requireActivity() as MainActivity).socketService.sendMessage(Gson().toJson(message))
        chatViewModel.insertChat(message)
        binding.inputMessage.text = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity() as MainActivity).binding.smoothBottomBar.visibility = View.GONE
    }

    override fun onDestroy() {
        (requireActivity() as MainActivity).binding.smoothBottomBar.visibility = View.VISIBLE
        super.onDestroy()
    }
}