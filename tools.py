# -*- coding: utf-8 -*-
"""
Created on Thu Apr 30 07:57:50 2015

@author: dantidot
"""

############################# Cluster plot
import igraph
from igraph import *
import numpy as np

import pandas as pd
import matplotlib.pyplot as plt

sim = ["sameCascadeSim", "timeStepSim", "SuccesseurSim", "PredecessorSim"]
simi = ["jacquart", "cosine", "successeur", "pred"]
y={}
y['MAP'] = "moyenne_MAP__ignoreInit" 
y['F1'] = "moyenne_F1.0-Measure_ignoreInit"
y['PRECISION'] = "moyenne_Precision_ignoreInit_zeroIfNoDiffusion"
y['L'] = "moyenne_LogLikelihood"
y['nbHyp'] = "moyenne_NbHypContaminated"
y['nbRef'] = "moyenne_NbRefContaminated"

def openGraph(sim, k, size = 1) :
    user = "cluster/Kmean_"+ sim + "_k_"+ k + ".csv"
    fo = open(user,"r")

    edge = []
    es = []
    vs = []
   
    graph_file = fo.read()
    print graph_file
    graph_file = graph_file.split("\n")
    vsn = []
    print graph_file
    for i in range(1, len(graph_file) - 1):
        vertice = graph_file[i].split(",")
        vertice_successor = vertice[3].replace("[","").replace(";]","").replace("]", "").split(";")
        vs.append (int(vertice[2]))
        vsn.append("a"+vertice[0])
        for j in range (len(vertice_successor)):
            if (vertice_successor[j] != ""):
                successor_information = vertice_successor[j].split(":")
                edge.append((i, int(successor_information[0])))
                es.append(float(successor_information[1]))
    g = Graph(edge)
    g.es["weight"] = es
    vsa = np.array (vs)
    
    vs = vs/(np.average(vsa)/float(10))
    if (size == 1):   
        g.vs["size"] = vs
    g.vs["label"] = vsn 
    return g
    
ax = False
def plot_exp (simI, tres, toPlot, experience, axReset = False, learning = "ic", base = "test"):
    if base == "test":
        exp1 = pd.read_csv(""+learning+"/result/csv/experience"+experience+"_sim"+simI+"_"+sim[int(simI)]+ ".csv",sep=',')
    else:
        exp1 = pd.read_csv(""+learning+"/result/csv/experience"+experience+"_sim"+simI+"_"+sim[int(simI)]+"_train" + ".csv", sep=",")    
    exp1 = exp1.dropna()
    global ax    
    if (axReset == False):
       ax = exp1.plot(x="k", y=y[toPlot], ax=ax)
    else:
        ax = exp1.plot(x="k", y=y[toPlot])
    return toPlot+" "+ "exp " + experience + " : " + learning
   
   


def plot_exp_2 (simI, tres, toPlot, experience, axReset = False, learning = "ic", base = "test"):
    if (tres == "0"):
        tres = ""
    else:
        tres = "_" + str (tres)
    sims = ""
    for simi in simI:
        sims += "_" + sim[simi]
    if base == "test":
        exp1 = pd.read_csv(""+learning+"/result/csv/experience"+experience+"_sim"+sims+ tres +".csv",sep=',')
    else:
        exp1 = pd.read_csv(""+learning+"/result/csv/experience"+experience+"_sim"+sims+"_train" + tres + ".csv", sep=",")    
    
    exp1 = exp1.dropna()
    global ax    
    if (axReset == False):
       ax = exp1.plot(x="k", y=y[toPlot], ax=ax)
    else:
        ax = exp1.plot(x="k", y=y[toPlot])
    return toPlot+" "+ "exp " + experience + " : " + learning
   
def plot_graph (sim, k, size = 1):
    simi = ""
    for simies in sim.keys():
        simi = simi + "_" + tools.sim[simies] + "." + sim[simies]
    g = tools.openGraph (simi, k, size = size).as_directed()
    g.write_graphml("graph/" + simi + k + ".graphml")
    return g

def plot_users (sim, k):
    sims = ["sameCascadeSim", "timeStepSim", "SuccesseurSim", "PredecessorSim"]
    simi = ""
    for simies in sim.keys():
        simi = simi + "_" + sims[simies] + "." + sim[simies]
    user = "cluster/Kmean_"+ simi + "_k_"+ str(k) + ".csv"
    user = pd.read_csv (user)
    user.plot (x="cluster", y = "users", kind = "bar")


def plot_users_cascade (sim, k , tres = 0):
    if (tres == 0):
        tres = ""
    else:
        tres = "_" + str (tres)
        
    sims = ["sameCascadeSim", "timeStepSim", "SuccesseurSim", "PredecessorSim"]
    simi = ""
    for simies in sim.keys():
        simi = simi + "_" + sims[simies] + "." + sim[simies]
    user = "cascade/Kmean_"+ simi + "_k_"+ str(k) + tres + ".csv"
    user = pd.read_csv (user)
    user.nb = user.nb / (19172.0 + 8072.0)
    user.plot (x="cluster", y = "nb", kind = "bar")
    
def plot_users_cascade_information (sim, k, info = "mean", normalize = False, tres = 0):
    if (tres == 0):
        tres = ""
    else:
        tres = "_" + str (tres)
        
    sims = ["sameCascadeSim", "timeStepSim", "SuccesseurSim", "PredecessorSim"]
    simi = ""
    for simies in sim.keys():
        simi = simi + "_" + sims[simies] + "." + sim[simies]
    user = "cascadeinformation/Kmean_"+ simi + "_k_"+ str(k) + tres + ".csv"
    user = pd.read_csv (user, sep = " ")
    if normalize:    
        user[info] = user[info] / (19172.0 + 8072.0)
    user.plot (x="cluster", y = info, kind = "bar")
    return user
    
def plot_average (sim, info = "mean", normalize = False, reset = True, color = "green"):
    global ax
    sims = ["sameCascadeSim", "timeStepSim", "SuccesseurSim", "PredecessorSim"]
    simi = ""
    
    for simies in sim.keys():
        simi = simi + "_" + sims[simies] + "." + sim[simies]
    step = 10
    x = []
    y = []
    k = 10
    while True:
        if k > 1113:
            break
        if k>100:
            step = 100
        user = "cascadeinformation/Kmean_"+ simi + "_k_"+ str(k) + ".csv"
        user = pd.read_csv (user, sep = " ")
        if normalize:    
            user[info] = user[info] / (19172.0 + 8072.0)
        x.append(k)
        y.append(user[info].mean())
        k = k + step
    
    
    if(reset == False):
        ax = ax.twinx()
        ax.plot(x,y, color = color)
    else:
        plt.plot(x,y)



def plot_std (sim, normalize = False, reset = True, color = "green"):
    global ax
    sims = ["sameCascadeSim", "timeStepSim", "SuccesseurSim", "PredecessorSim"]
    simi = ""
    
    for simies in sim.keys():
        simi = simi + "_" + sims[simies] + "." + sim[simies]
    step = 10
    x = []
    y = []
    k = 10
    while True:
        if k > 1113:
            break
        if k>100:
            step = 100
        user = "cascadeinformation/Kmean_"+ simi + "_k_"+ str(k) + ".csv"
        user = pd.read_csv (user, sep = " ")
        if normalize:    
            user[info] = user[info] / (19172.0 + 8072.0)
        x.append(k)
        y.append(tools.np.array(user)[:,2].max())
        k = k + step
    
    
    if(reset == False):
        ax = tools.ax.twinx()
        ax.plot(x,y, color = color)
    else:
        plt.plot(x,y)


def instability (sim, mink = 10, maxk = 1200, step = 100,reset = True, color = "green"):
    x = {}
    simi = ""
    for simies in sim.keys():
        simi = str(simi) + "_" + str(tools.sim[simies]) + "." + str(sim[simies])
    
    for i in range (mink, maxk, step):
        user = "cluster/Kmean_"+ simi + "_k_"+ str(i) + ".csv"
        user = pd.read_csv (user)
        x[i] = user.users.std()
    y = []
    xpl = sorted(x.keys())
    for i in xpl:
        y.append(x[i])
    
    if(reset == False):
        ax = tools.ax.twinx()
        ax.plot(xpl,y, color = color)
    else:
        plt.plot(xpl,y)
    return simi
    
    
    
def mean (sim, mink = 10, maxk = 1200, step = 100,reset = True, sucre = False, color = "green"):
    x = {}
    simi = ""
    for simies in sim.keys():
        simi = str(simi) + "_" + str(tools.sim[simies]) + "." + str(sim[simies])
    
    for i in range (mink, maxk, step):
        user = "cluster/Kmean_"+ simi + "_k_"+ str(i) + ".csv"
        user = pd.read_csv (user)
        if (sucre == True):
            x[i] = user[user.users < 1000].users.mean()
        else:
            x[i] = user.users.mean()
    y = []
    xpl = sorted(x.keys())
    for i in xpl:
        y.append(x[i])
    
    if(reset == False):
        ax = tools.ax.twinx()
        ax.plot(xpl,y, color = color)
    else:
        plt.plot(xpl,y)
    return simi
    

    
def comtocom (sim, k):
    simi = ""
    for simies in sim.keys():
        simi = str(simi) + "_" + str(tools.sim[simies]) + "." + str(sim[simies])
    user = "ic/user_Kmean_"+ simi + "_k_"+ str(k)
    f = open (user, "rb")
    f.readline()
    f.readline()
    line = f.readlines()
    f.close()
    comtocom = tools.np.zeros([k,k])
    for com in line:
        l = com.replace("\n","").split("\t")
        comtocom[int(l[0][13:])-1][int(l[1][13:])-1] = l[2]
    plt.figure()
    plt.imshow(comtocom, interpolation='nearest')
    plt.colorbar()
    return comtocom



    
def comtocomsuc (sim, k):
    simi = ""
    for simies in sim.keys():
        simi = str(simi) + "_" + str(tools.sim[simies]) + "." + str(sim[simies])
    user = "user/Kmean_"+ simi + "_k_"+ str(k)
    f = open (user, "rb")
    f.readline()
    f.readline()
    line = f.readlines()
    f.close()
    comtocom = tools.np.zeros([k,k])
    for com in line:
        l = com.replace("\r","").split(" ")
        comtocom[int(l[0][13:])-1][int(l[1][13:])-1] = l[2]
    plt.figure()
    plt.imshow(comtocom, interpolation='nearest')
    plt.colorbar()
    return comtocom    
