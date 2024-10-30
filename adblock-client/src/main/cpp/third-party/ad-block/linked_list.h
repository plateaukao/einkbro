//
// Created by Edsuns on 2021/1/16.
//

#ifndef LINKED_LIST_H
#define LINKED_LIST_H

template<class T>
class Node {
public:
    Node(T *_data) : data(_data), next(nullptr) {}

    ~Node() {
        delete data;
    }

    T *data;
    Node<T> *next;
};

template<class T>
class ListIterator {
public:
    ListIterator(Node<T> *_node) : node(_node) {}

    bool operator!=(const ListIterator<T> &iterator) {
        return node != iterator.node;
    }

    ListIterator<T> operator++() {
        if (node) {
            node = node->next;
        }
        return *this;
    }

    T operator*() const {
        return *node->data;
    }

    T operator->() const {
        return node->data;
    }

protected:
    Node<T> *node;
};

template<class T>
class LinkedList {
public:
    LinkedList() : head(nullptr), tail(nullptr), size(0) {}

    ~LinkedList() {
        clear();
    }

    void push_back(const T &item) {
        if (!tail) {
            head = new Node<T>(new T(item));
            tail = head;
        } else {
            tail->next = new Node<T>(new T(item));
            tail = tail->next;
        }
        size++;
    }

    void append_back(T *item) {
        if (!tail) {
            head = new Node<T>(item);
            tail = head;
        } else {
            tail->next = new Node<T>(item);
            tail = tail->next;
        }
        size++;
    }

    void concat(LinkedList<T> *other) {
        if (other) {
            Node<T> *temp = other->head;
            while (temp) {
                push_back(*temp->data);
                temp = temp->next;
            }
        }
    }

    void clear() {
        Node<T> *item = head;
        while (item) {
            Node<T> *temp = item;
            item = item->next;
            delete temp;
        }
        size = 0;
    }

    unsigned int length() const {
        return size;
    }

    ListIterator<T> begin() const {
        return ListIterator<T>(head);
    }

    ListIterator<T> end() const {
        return ListIterator<T>(nullptr);
    }

protected:
    Node<T> *head;
    Node<T> *tail;
    unsigned int size;
};

#endif //LINKED_LIST_H
