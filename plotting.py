# -*- coding: utf-8 -*-
"""
Created on Fri Apr 24 17:10:56 2015

@author: dantidot
"""

import pandas as pd
import matplotlib.pyplot as plt
import tools



#################################### PLOT FIG 1 #########################
################### PLOT PERF
legend = []
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset=True) + "_mu1")
legend.append(tools.plot_exp_2([1],tres = "0", toPlot = "MAP", experience = "1", learning = "ic") + "_mu2")
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "ic") + "_mu3")
legend.append(tools.plot_exp_2([3],tres = "0", toPlot = "MAP", experience = "1", learning = "ic") + "_mu5")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
legend.append("user ref")
# legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink"))
sim = []
plt.legend(legend)
plt.show()



legend = []
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset=True) + "_mu3")
legend.append(tools.plot_exp_2([3],tres = "0", toPlot = "MAP", experience = "1", learning = "ic") + "_mu5")
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink") + "_mu3")
legend.append(tools.plot_exp_2([3],tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink") + "_mu5")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
legend.append("user ref")
# legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink"))
sim = []
plt.legend(legend)
plt.show()


legend = []
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "MAP", experience = "5", learning = "ic", axReset=True) + "_mu1")
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "5", learning = "ic") + "_mu3")
#legend.append(tools.plot_exp_2([3],tres = "0", toPlot = "MAP", experience = "5", learning = "ic") + "_mu5")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
legend.append("user ref")
# legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink"))
sim = []
plt.legend(legend)
plt.show()


plt.show()



legend = []
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "5", learning = "ic", axReset=True) + "_mu3 0.01")
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "ic") + "_mu3")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
legend.append("user ref")
plt.legend(legend)
mean({2:"1.0"}, reset = False, color = "red")
plt.legend(["Taille Moyenne "], loc = 2)
instability({2:"1.0"}, reset = False, color = "black")
plt.legend([ "Variance"], loc = 5)
# legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink"))
sim = []
plt.show()


tools.plot_users({2:"1.0"}, 50)
tools.plot_users_cascade({2:"1.0"}, 50)
tools.plot_users_cascade({2:"1.0"}, 50, tres = 0.01)


tools.plot_users({2:"1.0"}, 210)
tools.plot_users_cascade({2:"1.0"}, 210)
tools.plot_users_cascade({2:"1.0"}, 210, tres = 0.01)





tools.plot_users({0:"1.0"}, 50)
tools.plot_users_cascade({0:"1.0"}, 50)
tools.plot_users_cascade({0:"1.0"}, 50, tres = 0.01)


tools.plot_users({0:"1.0"}, 210)
tools.plot_users_cascade({0:"1.0"}, 210)
tools.plot_users_cascade({0:"1.0"}, 210, tres = 0.01)


legend = []
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "MAP", experience = "5", learning = "ic", axReset=True) + "_mu1 0.01")
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "MAP", experience = "1", learning = "ic") + "_mu1")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
legend.append("user ref")
plt.legend(legend)
mean({0:"1.0"}, reset = False, color = "red")
plt.legend(["Taille Moyenne "], loc = 2)
instability({0:"1.0"}, reset = False, color = "black")
plt.legend([ "Variance"], loc = 5)
# legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink"))
sim = []
plt.show()






########## NAIV IC
legend = []
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset=True) + "_mu1")
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink") + "_mu1")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
legend.append("user ref")
sim = []
plt.legend(legend)
plt.show()
legend = []
legend.append(tools.plot_exp_2([1],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset=True) + "_mu2")
legend.append(tools.plot_exp_2([1],tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink") + "_mu2")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
legend.append("user ref")
sim = []
plt.legend(legend)
plt.show()
legend = []
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset=True) + "_mu3")
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink") + "_mu3")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
legend.append("user ref")
sim = []
plt.legend(legend)
plt.show()
legend = []
legend.append(tools.plot_exp_2([3],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset=True) + "_mu5")
legend.append(tools.plot_exp_2([3],tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink") + "_mu5")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
legend.append("user ref")
sim = []
plt.legend(legend)
plt.show()

################ PLOT INSTA
legend = []
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset=True) + "_mu1")
sim = []
legend.append(instability({0:1.0}, reset = False))
plt.legend(legend)
plt.show()
legend = []
legend.append(tools.plot_exp_2([1],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset=True) + "_mu2")
sim = []
legend.append(instability({1:1.0}, reset = False))
plt.legend(legend)
plt.show()
legend = []
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset=True) + "_mu3")
sim = []
legend.append(instability({2:1.0}, reset = False))
plt.legend(legend)
plt.show()
legend = []
legend.append(tools.plot_exp_2([3],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset=True) + "_mu5")
sim = []
legend.append(instability({3:1.0}, reset = False))
plt.legend(legend)
plt.show()



############################ MORE INSTA
tools.plot_users({0:"1.0"},k = 10)
plt.legend(["users mu1 k=110"])
tools.plot_users({0:"1.0"},k = 10)
plt.legend(["users mu1 k=310"])


tools.plot_users({1:"1.0"},k = 10)
plt.legend(["users mu2 k=110"])
tools.plot_users({1:"1.0"},k = 10)
plt.legend(["users mu2 k=310"])


tools.plot_users({2:"1.0"},k = 20)
plt.legend(["users mu3 k=110"])
tools.plot_users({2:"1.0"},k = 20)
plt.legend(["users mu3 k=310"])


tools.plot_users({3:"1.0"},k = 110)
plt.legend(["users mu5 k=110"])
tools.plot_users({3:"1.0"},k = 310)
plt.legend(["users mu5 k=310"])

################ PLOT CASCADE 
tools.plot_users({2:"1.0"},k = 20)
tools.plot_users_cascade({2:"1.0"},k = 110)

tools.plot_users({0:"1.0"},k = 20)
plt.legend(["k = 20"])
plt.show()
tools.plot_users_cascade({0:"1.0"},k = 20)
plt.legend(["k = 20"])
plt.show()

tools.plot_users({1:"1.0"},k = 20)
tools.plot_users_cascade({1:"1.0"},k = 20)


tools.plot_users({3:"1.0"},k = 10)
tools.plot_users_cascade({0:"1.0"},k = 100)


##### PLOT PERF SUCRE

legend = []
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "MAP", experience = "3", learning = "ic", axReset=True) + "_mu3")
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "MAP", experience = "1", learning = "ic") + "_mu3")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
legend.append("user ref")
sim = []
plt.legend(legend)
plt.show()


legend = []
legend.append(tools.plot_exp_2([1],tres = "0", toPlot = "MAP", experience = "3", learning = "ic", axReset=True) + "_mu3")
legend.append(tools.plot_exp_2([1],tres = "0", toPlot = "MAP", experience = "1", learning = "ic") + "_mu3")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
legend.append("user ref")
sim = []
plt.legend(legend)
plt.show()



legend = []
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "3", learning = "ic", axReset=True) + "_mu3")
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "ic") + "_mu3")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
legend.append("user ref")
sim = []
plt.legend(legend)
plt.show()


legend = []
legend.append(tools.plot_exp_2([3],tres = "0", toPlot = "MAP", experience = "3", learning = "ic", axReset=True) + "_mu3")
legend.append(tools.plot_exp_2([3],tres = "0", toPlot = "MAP", experience = "1", learning = "ic") + "_mu3")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
legend.append("user ref")
sim = []
plt.legend(legend)
plt.show()


############# MEAN

mean({0:1.0})
instability({0:1.0})

k = 100
tools.plot_users({0:"1.0"},k)
tools.plot_users_cascade({0:"1.0"}, k)
tools.plot_users_cascade_information({0:"1.0"}, k, "max")
tools.plot_users_cascade_information({0:"1.0"}, k, "mean")
tools.plot_users_cascade_information({0:"1.0"}, k, "std")



k = 210
tools.plot_users({2:"1.0"},k)
tools.plot_users_cascade({2:"1.0"}, k)
tools.plot_users_cascade_information({2:"1.0"}, k, "max")
tools.plot_users_cascade_information({2:"1.0"}, k, "mean")
tools.plot_users_cascade_information({2:"1.0"}, k, "std")

legend = []
legend.append(tools.plot_exp_2([1],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset = True) + "_mu3")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
tools.plot_average({1:"1.0"},"std", reset = False)
plt.legend(legend)


legend = []
legend.append(tools.plot_exp_2([1],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset = True) + "_mu3")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
plot_std({1:"1.0"}, reset = False)
plot_std({2:"1.0"}, reset = False)
plt.legend(legend)


########## Contingeance

tools.plot_users({1:"1.0"},50)
tools.plot_users_cascade({1:"1.0"},50)


tools.plot_users({0:"1.0"},50)
tools.plot_users_cascade({0:"1.0"},50)


tools.plot_users({3:"1.0"},50)
tools.plot_users_cascade({3:"1.0"},50)


tools.plot_users({2:"1.0"},50)
tools.plot_users_cascade({2:"1.0"},50)


t=comtocomsuc({2:"1.0"}, 60)
t=comtocomsuc({3:"1.0"}, 60)
t=comtocomsuc({1:"1.0"}, 60)
t=comtocomsuc({0:"1.0"}, 60)
t=comtocom({2:"0.5", 3:"0.5"}, 110)
tools.plot_users_cascade({3:"1.0"}, 10)
tools.plot_users_cascade({2:"1.0"}, 10)


legend = []
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset = True) + "_mu3")
#legend.append(tools.plot_exp_2([3],tres = "0", toPlot = "MAP", experience = "1", learning = "ic") + "_mu5")
#legend.append(tools.plot_exp_2([2,3],tres = "0", toPlot = "MAP", experience = "1", learning = "ic") + "_mu3_5")
legend.append(tools.plot_exp_2([2,3],tres = "0", toPlot = "MAP", experience = "6", learning = "ic") + "_mu3_5_notequi")
tools.ax.plot((0,1200),(0.41206209554738854,0.41206209554738854),color = "cyan")
legend.append("user ref")
# legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink"))
sim = []
plt.legend(legend)
plt.show()

tools.plot_users({2:"1.0"},10)
tools.plot_users_cascade({2:"1.0"}, 210, tres = 0.01)
tools.plot_users_cascade({2:"1.0"}, 210, tres = 0.0)


#######################################################################


legend = []
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset=True) + "_coCascade")
#legend.append(tools.plot_exp_2([1],tres = "0", toPlot = "MAP", experience = "1", learning = "ic") + "_time")
#legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset = True) + "_successor")
# legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink"))


sim = []
legend.append(instability({0:1.0}, reset = False))


plt.legend(legend)
plt.show()




legend = []
#legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset=True))
#legend.append(tools.plot_exp_2([1],tres = "0", toPlot = "MAP", experience = "1", learning = "ic"))
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "3", learning = "ic", axReset = True))
# legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink"))

plt.legend(legend)
plt.show()







legend = []
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "MAP", experience = "2", learning = "ic", axReset = True))
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "MAP", experience = "1", learning = "ic"))

sim = []
legend.append(instability({0:1.0}, reset = False))


plt.legend(legend)
plt.show()



legend = []
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "2", learning = "ic", axReset = True))
legend.append(tools.plot_exp_2([2],tres = "0", toPlot = "MAP", experience = "1", learning = "ic"))

plt.legend(legend)
plt.show()






legend = []
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "nbHyp", experience = "1", learning = "ic", axReset = True))
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "nbRef", experience = "1", learning = "ic"))

plt.legend(legend)
plt.show()

legend = []
legend.append(tools.plot_exp_2([0],tres = "0", toPlot = "MAP", experience = "1", learning = "ic", axReset = True))
plt.legend(legend)
plt.show()










legend = []

legend.append(tools.plot_exp_2([0,2],tres = "0", toPlot = "MAP", experience = "4", learning = "ic", axReset=True))
legend.append(tools.plot_exp_2([0,2],tres = "0", toPlot = "MAP", experience = "4", learning = "naiveLink"))
legend.append(tools.plot_exp("2",tres = "0", toPlot = "MAP", experience = "3", learning = "ic"))

plt.legend(legend)
plt.show()

###########

toPlot = "PRECISION"
legend = []
plt.figure()
legend.append(tools.plot_exp("0",tres = "0", toPlot = toPlot, experience = "1", learning = "ic", axReset=True))
legend.append(tools.plot_exp("1",tres = "0", toPlot = toPlot, experience = "2", learning = "ic"))
legend.append(tools.plot_exp("2",tres = "0", toPlot = toPlot, experience = "3", learning = "ic"))
#legend.append(tools.plot_exp("0",tres = "0", toPlot = "MAP", experience = "1", learning = "naiveLink"))
#legend.append(tools.plot_exp("1",tres = "0", toPlot = "MAP", experience = "2", learning = "naiveLink"))
#legend.append(tools.plot_exp("2",tres = "0", toPlot = "MAP", experience = "3", learning = "naiveLink"))

plt.legend(legend)
plt.show()


###########


############################# Cluster plot



########### GRAPHE ################
g = plot_graph({2:"1.0"},k="10",size = 1)
#########################################


################## Stat sur les user ##################
tools.plot_users({0:"1.0"},k = 110)


sim = []
plt.figure()
plt.subplot()
sim.append(instability({0:1.0}))
plt.legend(sim)

plt.show()