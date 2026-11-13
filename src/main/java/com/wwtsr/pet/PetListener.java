package com.wwtsr.pet;

/** Told when something a viewer would see has changed (observer pattern). */
@FunctionalInterface
public interface PetListener {

    void petChanged(Pet pet);
}
