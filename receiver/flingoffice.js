// External namespace for cast specific javascript library
var fling = window.fling || {};

// Anonymous namespace
(function() {
  'use strict';

  FlingOffice.PROTOCOL = 'urn:x-cast:com.infthink.cast.demo.office';

  /**
   * Creates a FlingOffice object.
   * @param {board} board an optional game board.
   * @constructor
   */
  function FlingOffice(board) {
    this.mBoard = board;

    console.log('********FlingOffice********');
    this.flingReceiverManager_ = fling.receiver.FlingReceiverManager.getInstance();
    this.flingMessageBus_ =
        this.flingReceiverManager_.getFlingMessageBus(FlingOffice.PROTOCOL,
        fling.receiver.FlingMessageBus.MessageType.JSON);
    this.flingMessageBus_.onMessage = this.onMessage.bind(this);
    this.flingReceiverManager_.onSenderConnected =
        this.onSenderConnected.bind(this);
    this.flingReceiverManager_.onSenderDisconnected =
        this.onSenderDisconnected.bind(this);
    this.flingReceiverManager_.start();
  }

  // Adds event listening functions to FlingOffice.prototype.
  FlingOffice.prototype = {

    /**
     * ready to fling pdf files
     */
    onFlingPdf: function(senderId, message) {
        console.log('****flingPdf: senderId:' + senderId + ' msg:' + JSON.stringify(message));
        this.mBoard.loadPdf(message.file);
    },

    /**
     * Sender Connected event
     * @param {event} event the sender connected event.
     */
    onSenderConnected: function(event) {
        console.log('onSenderConnected. Total number of senders: ' +
          this.flingReceiverManager_.getSenders().length);
    },

    /**
     * Sender disconnected event; if all senders are disconnected,
     * closes the application.
     * @param {event} event the sender disconnected event.
     */
    onSenderDisconnected: function(event) {
        console.log('onSenderDisconnected. Total number of senders: ' +
            this.flingReceiverManager_.getSenders().length);

        if (this.flingReceiverManager_.getSenders().length == 0) {
            window.close();
        }
    },

    /**
     * Message received event; determines event message and command, and
     * choose function to call based on them.
     * @param {event} event the event to be processed.
     */
    onMessage: function(event) {
        var message = event.data;
        var senderId = event.senderId;
        console.log('********onMessage********' + JSON.stringify(event.data));

        if (message.command == 'show') {
            this.onShow(senderId, message);
        } else if (message.command == 'leave') {
            this.onLeave(senderId);
        } else if (message.command == 'page') {
            this.onPage(senderId, message);
        } else {
            console.log('Invalid message command: ' + message.command);
        }
    },

    onShow: function(senderId, message) {
        this.onFlingPdf(senderId, message);
    },

    onLeave: function(senderId) {
        console.log('****OnLeave****');
    },

    onPage: function(senderId, message) {
        console.log('****onPage****');
        if (message.cmd == 1) {   // up 
            console.log('****onPage: up!' + document.body.scrollHeight);
            this.mBoard.goPrevious();
        } else if (message.cmd == 2) { //down
            console.log('****onPage: down!' + document.body.scrollHeight);
            this.mBoard.goNext();
        }
    },

    sendError: function(senderId, errorMessage) {
        this.flingMessageBus_.send(senderId, {
          'event': 'error',
          'message': errorMessage });
    },

    /**
     * Broadcasts a message to all of this object's known channels.
     * @param {Object|string} message the message to broadcast.
     */
    broadcast: function(message) {
        this.flingMessageBus_.broadcast(message);
    }

  };

  // Exposes public functions and APIs
  fling.FlingOffice = FlingOffice;
})();
