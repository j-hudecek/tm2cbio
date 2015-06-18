package org.transmart.tm2cbio

/**
 * Created by j.hudecek on 18-6-2015.
 *
 * SetList class is ordered unique O(1) access collection
 *
 */
class SetList<T> implements List<T> {
    private Set<T> set = []
    private List<T> list = []

    @Override
    int size() {
        list.size()
    }

    @Override
    boolean isEmpty() {
        list.isEmpty()
    }

    @Override
    boolean contains(Object o) {
        set.contains(o)
    }

    @Override
    Iterator<T> iterator() {
        list.iterator()
    }

    @Override
    Object[] toArray() {
        list.toArray()
    }

    @Override
    def <T1> T1[] toArray(T1[] a) {
        list.toArray(a)
    }

    @Override
    boolean add(T t) {
        if (!set.contains(t)) {
            set.add(t)
            list.push(t)
        }
    }

    boolean push(T t) {
        add(t)
    }

    @Override
    boolean remove(Object o) {
        set.remove(o)
        list.remove(o)
    }

    @Override
    boolean containsAll(Collection<?> c) {
        set.containsAll(c)
    }

    @Override
    boolean addAll(Collection<? extends T> c) {
        boolean  ret = false
        c.each { ret |= this.add(it) }
    }

    @Override
    boolean addAll(int index, Collection<? extends T> c) {
        boolean  ret = false
        c.each { ret |= this.add(index, it) }
    }

    @Override
    boolean removeAll(Collection<?> c) {
        set.removeAll(c)
        list.removeAll(c)
    }

    @Override
    boolean retainAll(Collection<?> c) {
        set.retainAll(c)
        list.retainAll(c)
    }

    @Override
    void clear() {
        set.clear()
        list.clear()
    }

    @Override
    T get(int index) {
        list[index]
    }

    @Override
    T set(int index, T element) {
        if (index < list.size())
            set.remove(list[index])
        set.add(element)
        list[index, element]
    }

    @Override
    void add(int index, T element) {
        set.add(element)
        list.add(index, element)
    }

    @Override
    T remove(int index) {
        set.remove(list[index])
        list.remove(index)
    }

    @Override
    int indexOf(Object o) {
        list.indexOf(o)
    }

    @Override
    int lastIndexOf(Object o) {
        list.lastIndexOf(o)
    }

    @Override
    ListIterator<T> listIterator() {
        list.listIterator()
    }

    @Override
    ListIterator<T> listIterator(int index) {
        list.listIterator(index)
    }

    @Override
    List<T> subList(int fromIndex, int toIndex) {
        subList(fromIndex, toIndex)
    }
}
