# TTR IT Landscape Technical Specification — Version 3.0

> TTR IT Landscape Technical Specification for Capacity Management
> RailNetEurope (RNE)
> Version 3.0 | 16.05.2024 | 142 Seiten
> Quelle: https://rne.eu/wp-content/uploads/TTR-IT-Landscape-Technical-specification-v3.0.pdf

---

TTR IT LANDSCAPE
TECHNICAL SPECIFICATION
(for the first step of the stepwise implementation)
Version   Description                               Author             Reviewed by            Date
Máté Bak,
## 0.1       The first draft version of the document   Seid Maglajlić,    TTR IT core team       27.06.2019
Mario Toma
Reviewed by the group. Inclusion of       TTR IT core team   1st review by RNE
## 0.2       feedback/proposals from the core                             TTR IT core team and   06.11.2019
group                                     TTR IT WG          TTR IT WG
Reviewed by the group. Group agreed
Reviewed and
to put this version on RNE website,
## 0.3                                                 TTR IT WG          agreed by TTR IT WG,   15.01.2020
with request to change the summary
at the meeting
completeness colors
Changed the completeness summary
colors in the agreed January’s version
## 0.4                                                 TTR IT WG          Informed               08.04.2020
of document regarding the proposal
from the meeting
Máté Bak,
Finished the advance planning part of
## 0.5       the document to be checked by the         Seid Maglajlić,                           02.06.2020
TTR IT core team
Mario Toma
Document updates according to the
## 0.6       feedback received from the TTR IT WG      TTR IT WG          TTR IT WG              15.12.2020
members
Máté Bak,
Document updates according to the
Reviewed by TTR IT
## 0.7       feedback received from the TTR IT WG      Seid Maglajlić,                           14.01.2021
WG
members
Mario Toma
Máté Bak,
Document updates according to the
## 0.8       process description and answers from      Seid Maglajlić,                           04.03.2021
the Process group
Mario Toma
Reviewed by the TTR IT WG. Inclusion                         Reviewed and
## 1.0       of feedback/proposals from the TTR IT     TTR IT WG          agreed by TTR IT WG,   22.04.2021
WG                                                           at the meeting
Restructure of the approved document
## 1.1       version 1.0, to be in line with the TTR   Mario Toma                                30.06.2021
Process structure
Version 1.1 approved by the RNE Gen-                         RNE General Assem-
## 2.0                                                 ./.                                       07.12.2021
eral Assembly on 7 December 2021                             bly
Document modification according to
the request form the DCM WG to up-
## 2.1                                                 Mario Toma                                08.02.2023
date the topics to be in line with the
process changes
## 2.2       Document approval by the DCM WG                              DCM WG                 10.10.2023
## 3.0       Version 2.2 approved by the RNE GA                           RNE GA                 16.05.2024
Table of Contents
## 1.     Introduction..................................................................................................................... 5
     1.1.    Purpose ................................................................................................................... 5
     1.2.    Scope ...................................................................................................................... 5
     1.3.    Document conventions ............................................................................................ 8
     1.4.    References ............................................................................................................ 10
## 2.     System architecture ...................................................................................................... 11
## 3.     Advanced planning (X-60 to X-11) ................................................................................ 13
     3.1.    Capacity publication - introduction ......................................................................... 13
     3.2.    Capacity Process Phases ...................................................................................... 13
       3.2.1.       Capacity Strategy ........................................................................................... 14
       3.2.2.       Capacity Model / Capacity partitioning (Advance planning) ............................ 15
       3.2.3.       Capacity Planning and Publication product portfolio (Capacity Supply) .......... 20
     3.3.    Capacity product messages .................................................................................. 22
     3.4.    TCR Messages ...................................................................................................... 26
       3.4.1.       TCR Message ................................................................................................ 27
       3.4.2.       TCRCanceledMessage .................................................................................. 39
       3.4.3.       CapacityProductSearchMessage.................................................................... 40
       3.4.4.       CapacityProductMessage ............................................................................... 42
     3.5.    Capacity process and capacity products ................................................................ 43
       3.5.1.       Identifiers ........................................................................................................ 43
       3.5.2.       Process .......................................................................................................... 44
       3.5.3.       Impact of using the capacity products ............................................................. 44
     3.6.    Central implementation .......................................................................................... 50
       3.6.1.       Access Control ............................................................................................... 50
     3.7.    National implementation ........................................................................................ 51
       3.7.1.       Use case overview ......................................................................................... 51
## 4.     Capacity Requests (X-11 to X+12) ............................................................................... 68
     4.1.    Annual requests .................................................................................................... 68
     4.2.    Rolling Planning requests ...................................................................................... 68
     4.3.    Ad hoc and short-term ad hoc requests ................................................................. 68
## 5.     Modules (Microservices) ............................................................................................... 69
     5.1.    Messaging module ................................................................................................ 70
       5.1.1.       Centralized master data.................................................................................. 71
       5.1.2.       Integration options .......................................................................................... 72
       5.1.3.       Connection to other modules .......................................................................... 73
       5.1.4.       Central implementation ................................................................................... 73
       5.1.5.       National implementation ................................................................................. 74
     5.2.    BigData module ..................................................................................................... 74
       5.2.1.       Centralized master data.................................................................................. 75
  5.2.2.     Integration options .......................................................................................... 76
  5.2.3.     Connection to other modules .......................................................................... 77
## 5.3.   Capacity Needs Announcements (CNA) ................................................................ 78
  5.3.1.     Centralized master data.................................................................................. 78
  5.3.2.     Integration options .......................................................................................... 79
  5.3.3.     Connection to other modules .......................................................................... 80
## 5.4.   Path Request Management (PRM) ........................................................................ 80
  5.4.1.     Centralized master data.................................................................................. 80
  5.4.2.     Integration options .......................................................................................... 81
  5.4.3.     Connection to other modules .......................................................................... 82
## 5.5.   Path Management (PM)......................................................................................... 82
  5.5.1.     Centralized master data.................................................................................. 83
  5.5.2.     Integration options .......................................................................................... 84
  5.5.3.     Connection to other modules .......................................................................... 85
  5.5.4.     Enhancement of the functionality .................................................................... 86
  5.5.5.     Central implementation ................................................................................... 87
  5.5.6.     National implementation ................................................................................. 87
## 5.6.   Temporary Capacity Restriction (TCR) ................................................................ 113
  5.6.1.     Centralized master data................................................................................ 113
  5.6.2.     Integration options ........................................................................................ 114
  5.6.3.     Connection to other modules ........................................................................ 116
  5.6.4.     Central implementation ................................................................................. 116
  5.6.5.     National implementation ............................................................................... 117
## 5.7.   Capacity Hub (CH) .............................................................................................. 118
  5.7.1.     Centralized master data................................................................................ 118
  5.7.2.     Integration options ........................................................................................ 120
  5.7.3.     Connection to other modules ........................................................................ 122
  5.7.4.     Enhancement of the functionality .................................................................. 123
## 5.8.   Capacity Broker (CB) ........................................................................................... 124
  5.8.1.     Introduction................................................................................................... 124
  5.8.2.     Www.rail-booking.eu concept ....................................................................... 125
  5.8.3.     Centralized master data................................................................................ 130
  5.8.4.     Integration options ........................................................................................ 130
  5.8.5.     Connection to other modules ........................................................................ 131
  5.8.6.     Functionality ................................................................................................. 132
## 5.9.   Sales (S) ............................................................................................................. 133
  5.9.1.     Centralized master data................................................................................ 134
  5.9.2.     Integration options ........................................................................................ 135
  5.9.3.     Integration options ........................................................................................ 136
  5.9.4.     Functionality ................................................................................................. 136
1. Introduction
    The feedback on the feasibility check questionnaire of the “TTR IT Landscape Analysis” doc-
ument showed that companies are not ready for the full TTR IT implementation. Therefore, the
TTR IT Landscape will be gradually implemented, to be ready for the TT2025. In this stepwise im-
plementation approach, only minimum requirements will be described and built into the solu-
tion, with the aspiration to complete implementation later.
       1.1. Purpose
     The main purpose of this document is to provide a detailed technical specification needed
for implementation of the TTR IT landscape (renamed to Digital Capacity Management - DCM).
    In the document, information on the technical interfaces, messages and communication
workflows will be provided. Furthermore, the overview of the needed implementation in Infra-
structure Managers (IMs) and Responsible Applicants (RAs) side will be given.
   This document is targeted to those people within railway companies and their suppliers,
who are in charge of organising, supervising and/or carrying out the implementation of the
functinalities of the TTR IT Landscape.
   The intended audience of this document are:
       •   TTR IT core team members / DCM Core Team members
       •   TTR IT WG members / DCM WG members
       •   RNE
       •   FTE
       •   TEG Planning (Technical expert group)
       •   Stakeholders (IMs, ABs and RAs)
       •   Existing suppliers that provide the systems mentioned in the document
       1.2. Scope
    This technical specification of the TTR IT Landscape outlines the implementation of the nec-
essary functions for the first step of implementing TTR IT Landscape. The specification covers
the national and central implementation on existing systems, as well as a definition proposal of
new capacity product objects and messages based on the TAF/TAP TSI standard and possible
modifications of existing objects.
This document covers the following topics:
    •    Definition of new TAF/TAP TSI messages and objects
    •    Modification of existing TAF/TAP TSI objects
    •    Description of the minimal required implementation together with use cases
o Description of central implementation
o Description of implementation with examples to be done at the national level
    •    Information about the basic functions that should be implemented in the second step
(additional requirements)
   The scope of this work includes further development on the existing systems, based on the
process implementation and IT Landscape documents, taking into account feedback from the
TTR Pilots.
Example description
    All the examples and use cases in the document will reference to the simple railway infra-
structure example that shows a border crossing between Austria (ÖBB) and Hungary (VPE).
Each side has three primary locations, including also the state or network borders. One of the
IM works alone, but other sends its capacity to an RFC.
Figure 1 - Railway infrastructure that will be used in the document
   Also, as it is shown on the image, in Hungary RFC7 will be active in the capacity product
publication. As in TAF TSI messages, the companies are identified with their company code, but
RFC7 doesn’t have one yet, we’ve added there a 4-digit identifier (actually, it’s the corridor ID in
PCS).
The summary of completeness
    To facilitate completeness checks and the possibility to use a particular topic it in the na-
tional implementation plan, at the beginning of each topic a table is defined that describes the
degree of completion, open issues, reasons and steps to be taken to finish the topic.
    To the faster visualization of the completeness of each topic, the different colours were de-
fined.
    COLOR                   DESCRIPTION
IM      RNE
The content of this topic is drafted by RNE
TEG/SMO   TTR IT
IM       RNE
The content of this topic is finalized by RNE and is under review by TTR
       TEG/SMO    TTR IT    IT core team and TTR IT WG members
IM      RNE
TEG/SMO    TTR IT   TTR WG IT has encountered blocking points to be solved
IM       RNE
TEG/SMO   TTR IT
TTR WG IT has triggered red means not accepted or the content of this
topic is not finished. It is just drafted, and further development should
be done.
The content of the topic is finished and can be used. It is possible that
some minor changes could be made, but they will not have an impact
IM       RNE
on the overall content. It is approved by the TTR IT group but not yet ap-
TEG/SMO   TTR IT
proved by the TEG and SMO groups.
            IM      RNE      The content for the topic is finished and approved by the TEG (and
SMO) group and can be completely used for the national implementa-
TEG/SMO   TTR IT
tion.
   The contents marked with the green and blue colours can be considered for the national im-
plementation of the TTR IT Landscape. These contents may be used for the preparation of the
necessary updates in national systems, to be in line with the TTR IT Landscape.
     The orange and especially red colorus are draft proposals and not mature enough to be con-
sidered for the central neither national implementation. It is still working on these topics and,
for the time being, should not be used as a final specification for the preparation of the com-
plete changes in national systems. These contents will be updated.
1.3. Document conventions
      Description of the most used acronyms is displayed in the table below.
 Acronym                     Explanation
RNE central database to enable the exchange of harmonized reference
 BigData
files between IM and RNE systems.
Capacity blocked in the capacity diagram to be used as a cushion against
 Buffer block
fluctuations in available capacity for train runs and TCRs
Time frame up to several hours that includes capacity for at least one path
 Capacity band               for rolling planning requests. Publication in the form of a number of "slots"
per defined capacity band
 Central TTR IT Frame-       Represents a common environment for all the modules defined in the TTR
 work                        IT Landscape. It consists of three layers: RU, IM and Common layer.
Common Interface, a message transformation middleware, which does
 CI                          the transformation from legacy format of messages into common or
shared metadata format (XML) and vice versa
Customer Information Platform – a web-based tool that provides precise
 CIP                         information on routing, terminals, specific track properties and infrastruc-
ture investment projects
 CMO                         Capacity Model Object – presents the traffic volumes provided by IMs
                    Capacity Needs Announcements – need for capacity from the Applicants
CNA
side
Central Reference Database File, formerly known as Central Repository
CRD                 Domain, a common repository node in the network containing the refer-
ence files
DCM                 Digital Capacity Management – digitalisation of the TTR IT Landscape
Data Warehouse – a system used for reporting and data analysis. It is a
DWH
central repository of integrated data from one or more disparate sources
ERA                 European Railway Agency
RNE webtool to visualise and manage the infrastructure data provided in
the CRD database. Also, some additional data that doesn’t exist in the
GeoEditor
CRD like segments, sections, layers are created and managed in the Ge-
oEditor.
Joint Sector Group – a voluntary organisation supported by 9 European As-
sociations involved in the implementation of the Technical Specification
JSG
for Interoperability relating to the subsystem “Telematics Applications for
Freight services” (TAF TSI of the rail system in the European Union
The layer is a set of sections (and section groups implicitly) that has a cer-
Layer
tain added value. One corridor can be one layer, for example.
Local Interface, a node in the network interfacing the legacy systems (CI
LI
from a user perspective)
Message status: Assigned by the Sender 1=creation, 2=modification,
MS
3=deletion
PCS                 Path Coordination System
The Process Implementation Sub-group (responsible for the description
PI SG
and interpretation of the TTR processes)
Primary location code is a basic topology unit. Location is uniquely de-
PLC
fined within the BigData system.
Proof of Concept - is a realization of a certain method or idea in order to
POC                 demonstrate its feasibility or demonstration that some concept or theory
has practical potential
This is a path that an IM has planned at the beginning of the capacity pro-
cess based on the cap. partitioning as well as its expectations regarding
market needs, requirements contained in Framework Agreements, and
Pre-planned paths
capacity needs announcements made by applicants. TCRs according to
the RNE guideline “TCR” have to be taken into account as much as possi-
ble.
RA                  Responsible Applicants
Rail Facility Portal – a web-based tool which provides a quick access to in-
RFP                 formation on all kinds of rail facilities, in particular rail freight facilities, for
the planning of rail services
    RINF                       Register of Infrastructure of European Union Agency for Railways (ERA)
Rail Information System – a system that will provide an up-to-date and
    RIS
complete description of the railway infrastructure
"Capacity usage possibility" within a capacity band that will be converted
    Rolling Planning Slot
into a path year after year
The section is an ordered set of segments. Each section connects two not
    Section                    necessary neighbouring points where there exists only one possible path
from one section’s edge point to another one.
The segment represents the link connecting two neighbouring primary lo-
    Segment
cations
Sector Management Office – responsible for Change Management in
    SMO
TAF/TAP TSI schema
Subsidiary location is hierarchically child of the primary location. One
    Subsidiary location
subsidiary location can have only one parent primary location.
Technical Specification for Interoperability (TSI) relating to Telematics Ap-
    TAF/TAP TSI
plication for Freight (TAF) and Passenger (TAP)
Type of Information: Enumeration, indicating to which process step or
    TOI
process type in the planning the message belongs
Type of Request: Enumeration for 3 different basic types of the processes
    TOR
in the planning: Study (1), Request (2), Modification (3)
    TTR                        Timetable Redesign (Project title)
The TTR IT Landscape describes the functionalities and modules of the fu-
    TTR IT Landscape
ture IT landscape. It is a result of the digitalisation of the processes.
Capacity on a line that is still available after pre-planned capacity for ATT
    Unplanned capacity
and RP traffic as well as TCRs (incl. maintenance) have been assigned
A deadline referring to the day of the timetable change (X) and the number
    X-n
of months (n) in advance of this deadline.
1.4. References
       The bases for the preparation of this document were the following documents:
•   TTR IT Landscape Analysis – version 1.0 from 20th November 20181
1
    TTR IT Landscape Analysis document
            •    Description of the Timetabling and Capacity Redesign Process – version 3.0 from 7th
December 20212
•    TTR IT Landscape technical specification – version 2.0 from 7th December 20213
•    Common Components System LI Release 2.0 User Manual – version 2.0.1 from 18th
October 2018
•    BigData GeoEditor user manual
## 2. System architecture
    In general, the architecture of the Central TTR IT framework, from the stakeholder's point of
view, is quite simple. IMs and RAs national systems will communicate using the Common inter-
face (CI) with the Central TTR IT Framework.
Figure 2 - Simplified IT architecture
    IMs and RAs have to set up CI on their national levels. Information on the setup of the CI can
be found in the Messaging module under the national implementation description and in Annex
5 of this document.
2
    Description of the Timetabling and Capacity Redesign Process
3
    TTR IT Landscape technical specification
    It will be considered to extend the functionality of the CI to exchange json files among the
stakeholders, to be prepared to support the data exchange among various type of systems (in-
cluding mobile devices).
   The Central TTR IT framework is divided into three layers that contain relevant modules.
These three layers are: IM layer, RA layer and Common layer.
   The modules that will be used by IMs, inside the IM layer, are the following:
       •   TCR module (TCR)
       •   Path Management module (PM)
       •   Capacity Hub (CH)
       •   Capacity Broker (CB)
   The modules that will be used by RAs are the following:
       •   Capacity Hub (CH)
       •   Path Request Management module (PRM)
Capacity Needs Announcements (CNA) module will not be developed as a separate
   module, but its functionalities will be integrated into the Capacity Hub. The structured Excel
   file to announce the capacity needs from RAs will be used. According to that, an import
   functionality in the Capacity Hub will be developed to import CNA data from the Excel files.
   In addition, the capacity messages for data exchange are defined and shall be developed on
   both, central and national, sides.
   The modules that will be used by both (IMs and RAs) are following:
       •   Messaging module (Common interface shall be used)
       •   BigData module (RIS – Railway Infrastructure System in the future)
    IMs should implement the CI on their side to establish the communication with the central
system.
    In the central TTR IT Framework, there are more tools (modules) that are connected and that
have their own functionalities, but from the IMs and RAs perspective, it should work as a one
system with the one point of connection.
    The detailed diagram of the architecture of the Central TTR IT Framework and data flow, for
the first implementation step, is presented in the Annex 3 of this document.
3. Advanced planning (X-60 to X-11)
Advanced planning means that capacity needs to be planned and partitioned before the request
phase begins. Advanced planning takes into account market needs and available capacity, but
also the temporary capacity restrictions (TCRs).
3.1. Capacity publication - introduction
    When it comes to capacity, we mean positive and negative capacity.
    The positive capacity concerns all free usable capacities in a network not already booked or
allocated respectively blocked and locked by TCR and other manners.
   The negative capacity is used to indicate the TCRs and another capacity that cannot be re-
quested like already allocated paths (booked or offered).
    A key factor to ensure the stability of international timetabling is the availability of capacity
and every IM has to build a clear picture of the available infrastructure on its network. The recom-
mendation is that IMs publish all types of capacities (national and international) dedicated to an-
nual planning, rolling planning, temporary capacity restrictions and maintenance windows (a
subgroup of TCRs that are planned frequently). This capacity information will be visualized in the
capacity supply in the Capacity Hub/Broker module. The more data is fed into the system, the
better answers will be provided from the Central TTR IT Framework.
3.2. Capacity Process Phases
   The overview on the entire TTR Process is provided in the long version of the redesigned TT
process, version 3.0 from 7th December 2021 (the link to the TTR Process description).
     The description of each process step is not part of this chapter for the reason that the text
listed in the TTR Process document is not duplicated. This chapter covers the required IT imple-
mentation following the description of the TTR process.
   The components described in this chapter constitute the central building blocks of the pro-
cess.
1.1.1   3.2.1. Capacity Strategy
     A capacity strategy represents an international harmonization of events with a major impact
on capacity availability (such as prolonged track closure, intended increase of commuter ser-
vices or newly opened lines, etc.). It is a precondition for the development of a capacity model for
a line, a part of the network or the entire network. For cross-border lines, the capacity strategy,
including TCRs, needs to be shared with the neighbouring IM(s).
    The capacity strategy describes the main principles to be used in the planning of elements in
the capacity model and they are summarised in the text document.
    The capacity strategy creation starts 60 months before the timetable change (X-60) and end-
ing 36 months before the TT change, leading to the capacity model. Items influencing the strat-
egy should be communicated in the level of detail needed for a basic plan.
    Data to be delivered
    Information that IMs have to provide are the following:
    o   Annual capacity analyses on the capacity usage and evaluation of the of potential addi-
tional requirements,
    o   Information on how the capacity investments is scheduled for their infrastructure, in-
cluding major TCRs, to ensure efficiency in timing and availability of (alternative) capac-
ity,
    o   Check the possible increase/decrease of traffic for the timetable period.
    Tools (modules) to be used
    In the first releases of the Capacity Strategy, for the TT2025 to 2027, no IT is required. The
strategies will be prepared in text form and exchange among the IMs.
    In the later TT periods (after TT2027) when IMs become more experienced in the creation of
the capacity strategies and capacity models, the capacity strategy version could be prepared in
the system. The following TTR IT modules could be used:
o   TCR module – to provide available information about the expected major impact
TCRs in the capacity strategy,
▪
     For the later years (after 2025), the creation will start with the review of the previous year’s
capacity strategy (with included information on known major TCRs). The first version of the pos-
sible cap. Strategy shall be exchanged with neighbouring IMs and then with other stakeholders.
It should be updated according to the received feedback and at X-36, the final version will be
used for publication in NS for the related timetable and it will be the basis for starting elabora-
tion of the capacity model.
## 1.1.2   3.2.2. Capacity Model / Capacity partitioning (Advance planning)
    The capacity model, in the sense of the TTR process, represents a single entity of consolida-
tion of all known capacity elements like available capacity, capacity needs announcements, ex-
pected traffic volume, TCRs and so on. This model is a reference diagram with additional features
in which all data regarding a specific timetable period will be incorporated. The findings from the
capacity strategy will be included as well as applicants’ capacity need announcements.
    The capacity model sets the volumes of the transport per each market segment and the share
of TCRs on a specific line specified per direction. Finetuned detailed train paths are not supposed
to be part of the capacity model. It consists of two part – TCR share overview and 24h overview of
traffic volumes reflecting market needs.
  For the TT2025, it will be manually added to the capacity Hub (ECMT) and TCR (TCR Tool)
modules. These modules are described in the chapters 4.6. and 4.7. of this document.
    For the TT2026 and further, the assumption is that the creation of the capacity model will
start by providing data considering the capacity model from the previous year and then update
the capacity model with necessary data by uploading them into the capacity Hub and TCR mod-
ules via TSI based messages (chapter 3.2. and 3.3. of this document). IMs will create a Capacity
Model by sending data from their national systems yearly, as their systems are the source of
data.
    Details on data creation and exchange are provided in a separate document, as annexes to
the Process handbooks.
    The creation of the capacity model starts 36 months before timetable change (X-36) under
the leads of IMs and last until 18 months prior the timetable change. The capacity model is de-
fined for each international line individually and serves as the baseline for all capacity requests.
   Data to be delivered
   As the source of data to be added to the capacity model came from the following sources:
   -   Capacity model from the preceding timetable year (if exists)
   -   Capacity strategy for the referenced timetable year (a text document, revised and up-
       dated yearly)
   -   Planned TCRs with the major and high impact for the referenced timetable year
   -   Capacity needs announcements from the Applicants with a new market input
   -   Own hypothesis about market growth
   The objects that capacity model contains:
   o   Line – part of the network of one IM or part connecting the networks of two IMs. The lines
       are defined in the RNE BigData and will be synchronized with the Capacity Hub. All the
       necessary line updates (including the updates of other infrastructure data like PLCs,
       Companies, etc.) will be made in the CRD.
   o   Market segment - expected traffic type on a given line. This information will be provided
       directly in the Capacity Hub manually or by importing using messages defined in the
       chapter 3.2. of this document, or via pre-defined Excel file (like capacity needs announce-
       ments data which structure is provided in the Annex 8.
   o   TCRs – update information on known TCRs with major/high impact and new market inputs
   o   Expected traffic volume – represents the volume of paths that are expected by the IM
       which will be needed for ATT requests.
   o   Expected volume of Rolling Planning (RP) requests – represents the number of paths that
       IMs expect that will be needed for Rolling Planning requests. The already allocated multi-
       annual capacity in the rolling planning requests of previous TT periods, must be included
       in the capacity model.
   o   Expected volume of Ad-hoc requests – it should be defined especially on networks with
       an increased volume of ad-hoc traffic (e.g., capacity requirements that cannot be
       planned in advance, capacity can also be partitioned for this kind of traffic).
   o   Unplanned capacity - Only known capacity demands shall be pre-planned in advance and
       leftover capacity (empty space) might represent unplanned capacity
   ▪
    The capacity model/partitioning of a line should occasionally be updated until X-18, based on
the inputs related to TCRs, capacity need announcements from Applicants and IMs’ own hypoth-
esis regarding the traffic growth and experience gained from market developments.
   Tools (modules) to be used
    For the capacity model valid only for the TT2025 (the first mandatory year to publish), neces-
sary data shall be created either manually in the ECMT or imported via Excel file structure devel-
oped for the ECMT import/export.
   In the case that the interfaces and messages explained in the chapter 3.2. are available and
implemented by IMs (after TT2025), the IMs can deliver data using the messages as well.
    The capacity model creation could start by copying the capacity model of the previous year.
This should be done in IMs national systems, as they are the source of the data. When data is
prepared, they should be imported into the Capacity Hub (ECMT).
    In addition, to support smaller Applicants who do not have their own systems for managing
CNAs, the Capacity Hub (ECMT) should have carried forward functionality, to copy CNA objects
from the previous year.
    After this, the model data should be updated considering the information from the capacity
strategy for the considered reference timetable. Besides that, information on known TCRs with
major/high impact and a new market need delivered by Applicants should be added. The IM‘s own
hypothesis on market growth should be included as well.
   The capacity models with the updated information should be exchanged among neighbours,
especially a development on cross-border lines.
   The following TTR IT modules shall be used:
       o   Capacity Hub module (ECMT) – will be used for the creation of the capacity model.
The infrastructure data should be synchronized with the BigData database, which is
the prerequisite for capacity model creation. The following functionalities are consid-
ered:
▪ Create/update capacity model manually or by Excel data import for the TT
2025, and data exchange by using capacity messages (chapter 3.2.) for the
later timetable years,
▪ Make a copy of the previous capacity as a basis state for a new capacity model
(carry forward of CNAs),
               ▪  Supports all necessary capacity elements and allows coordination via com-
menting functions and tracks,
▪ Supports publication of capacity models and possibility to display models for
different levels of granularity,
▪ Import of capacity needs announcements (CNA) data by Applicants via the
Excel file structure, defined by the FTE IT WG. The CNA Excel file will be im-
ported directly to the Capacity Hub, without additional interface (only stand-
ardized Excel template).
▪ Update information on capacity model objects considering the CNA refer-
ences
       o   TCR (TCR Tool) – used as the source for the major and high impact TCRs
▪ For a new major/high impact TCRs created in the TCR module
▪ Updates of already existing TCRs in the capacity model with major and high
impact
▪ The preliminary data exchange with neighbouring IMs and TCR coordination
       o   RIS (which incorporates data from CRD, GeoEditor, RFP, CIP, RNE BigData, while us-
ing RINF data as much as possible)– Used as a central database for all infrastructure
data needed for DCM (ex TTR IT Landscape) modules.
▪ All data must be updated from the CRD database before starting with the ca-
pacity model creation,
▪ Additional specific information related to the RIS should be checked and up-
dated if necessary (like border points, segments, sections, etc.)
       o   Capacity Needs Announcements (Excel file) – used as a first information on Appli-
cants needs
▪ Data shall be delivered by CNA Excel file (see Annex 8)
       o   Messaging module (CI) – has to be implemented and should serve for data exchange
between national and central systems. The messages that should be concerned are
the capacity and TCR messages.
    For better visualisation of the capacity model data, data shall be presented on the corre-
sponding lines. Only the volume (amount) of the transport per each market segment and per cer-
tain timeframe should be shown, including a basic operational information like train category,
maximum weight and length, speed, dangerous goods and extraordinary consignments.
   The implementation of the capacity model and its visualisation could be provided as pre-
sented on the image below:
Figure 3 - Possible presentation of the Capacity Model for the part of line
  It is necessary to understand the difference between the capacity analysis and capacity
model.
   The capacity analysis is the method or simulation of calculation of various capacities, to be
used for checking different scenarios of capacity utilisation. Various tools can be used by IMs
and stakeholders. It is a “tool” for the capacity model preparation.
    The Capacity Model present the draft and/or final output, showing how the capacity should
be used by each market segment and for TCR purposes.
Basic requirements for a published capacity model:
    1. The Capacity Models shall be prepared for the complete network
    2. The capacity partitioning shall be done at least for a timetable year and published per
       train path line section and direction.
    3. The publication shall be done via the Capacity Hub (ECMT), unless the IM already has an
       existing tool for Capacity Models, in that case, it can be done also via national tool and
       the interface for data exchange has to be developed as soon as possible.
    4. The TCRs implies to major and high impact TCRs (as published at X-24), estimated of ca-
       pacity and approximate placement of medium TCRs, minor TCRs, maintenance win-
       dows.
    5. The Annual timetable (ATT) requirements shall be presented separately for the passen-
       ger and freight paths. The expected number of slots for passenger regional and long-dis-
       tance trains, and freight trains on a standard weekday shall be provided.
    6. Expected number of slots for rolling planning (RP) on a standard weekday shall be pro-
       vided.
    7. Expected number of slots for ad-hoc on a standard weekday shall be provided.
    8. The expected number of slots (bands) should be added to the model to ensure the sta-
       bility of the model. It should be provided not only separately for a train path line section,
       but also for the real origins to destinations.
    To support all the necessary tasks to create the capacity model, it is not necessary to have
national tool for creation of the capacity model (especially for the first TTPs). All tasks can be
made in the ECMT, which will be the capacity model tool.
## 1.1.3   3.2.3. Capacity Planning and Publication product portfolio (Capacity Supply)
    Before the capacity planning starts, the Applicants will be consulted on various issues like
the intended capacity offer, Network Statement (NS), TCRs.
    The first step in the capacity planning, based on capacity analysis made in earlier stage, is
the consultation of TCRs with stakeholders.
    The second step is the finalisation of the capacity needs announcements and network
statement consultation with presentation of changes in comparison with the previous version
Applicants are invited to give feedback.
   The third step is related to the feasibility study and finally, publication of the capacity prod-
ucts itself.
    The capacity planning starts 18 months before timetable change (X-18), after the capacity
partitioning (commercially available part of the capacity model), as a final step of the capacity
model creation. The capacity planning and publication process phase finishes 11 months be-
fore the TT change with the publication of the capacity products (Capacity Supply) which can be
requested by Applicants.
    Data to be delivered
    Information that IMs have to provide are the following:
    -   Publication of all known TCRs according to the RNE TCR Guidelines (through the TCR
module)
   -   Possible design of the capacity products (e.g. catalogue paths, capacity bands) for use
       in the annual requests
   -   Capacity for ad-hoc requests can be made available – potentially as paths or slots safe-
       guarded for pre-planned paths or empty space for tailor-made requests
   -   Capacity bands for rolling planning capacity
   Tools (modules) to be used
   The following TTR IT modules may be used:
       o   TCR (TCR Tool) – used for the following functionalities:
▪ TCR Coordination on defined TCR types between the involved IMs
▪ Consultation on TCRs among the IMs and Applicants
▪ Publication of TCRs
▪ Exchange TCR data with the ECMT in sense of all capacity visualisation
       o   Capacity Hub (ECMT) – used for planning, visualisation and publication of the capac-
ity products (Capacity Supply role)
▪ Import information on positive capacity (catalogue paths, capacity bands)
according to the CMOs from the final Capacity Model, via IMs’ national sys-
tems
▪ Exchange data with the TCR Tool regarding the TCRs and provide information
on affected paths on the TCR Tool requests (information needed to easier
IMs the TCR coordination in the TCR Tool)
       o   Path Request Management (PCS) –
       o   Feasibility study – request a path for a study of how the train can realistically run,
that helps in creating path requests for tailor-made paths to have a preview on the
paths.Path Management module (PCS) -
▪ Feasibility study – study the path with Applicants, to reduce the effort in the
path elaboration with a realistic path offer.
▪ Information on booked and reserved paths
▪ Information on ad-hoc and tailor-made paths
    The ECMT (Capacity Supply) was developed to support TTR Pilots and its functionalities will
be further updated to support all the TTR Process requirements of the Capacity Supply.
Figure 4 - European Capacity Model Tool
Basic requirements for a published Capacity Supply (applicable for TT2025):
    1. Time-diagram with the 365-days overview of the capacity supply published per train-
       path-line section and direction with a zoom in/out possibility to a line and corridor.
    2. Complete network should be considered.
    3. The TCRs with the Major, High and Medium impact considered (as published at X-12) in-
       cluding the maintenance windows.
    4. For the ATT, any capacity product can be used (pre-planned paths, bandwidths, empty
       space for tailor-made requests). The cross-border capacity shall be harmonised.
    5. Pre-planned paths and/or bandwidths (with number of slots) for RP should be safe-
       guarded. The cross-border shall be harmonised.
    6. Pre-planned paths and/or bandwidths (with number of slots) for ad-hoc/short-term
       should be safeguarded. Empty space can be used for ad hoc requests, but in case the
       capacity is also safeguarded, this information should be part of the publication.
3.3. Capacity messages
    In the existing TAF/TAP TSI schema, the messages to manage capacity products do not exist.
Therefore, new messages should be defined, and a proposal of the capacity messages structure
is provided in this section.
   The messages will be approved by TTR IT WG and then sent to the Technical Expert Group
(TEG Planning), and Sector Management Office (SMO) for their final feedback. After that mes-
sages could be incorporated into the Joint Sector schema or TAF/TAP TSI schema.
    The capacity model is a description of a 24-h overview reflecting market needs and TCRs
with major/high impact. The aim of a capacity model is a definition of the demand forecast, di-
vided into an approximate share for commercial needs and TCRs (advanced planning).
    As it was described in the chapter 3.2.2., the focus of the capacity model is on the volumes
of the transport per each market segment and shares for TCRs and not on the real timings, real
paths, etc.
    The next step, after the capacity model is finalised (at X-18), is the capacity planning and
publication and for this step a more detailed information is needed. From the capacity model,
IMs need to transform expectations about future demand into capacity products that can be
planned, safeguarded, and offered to customers. All the elements that can be requested by Ap-
plicants, have to be shown in a capacity diagram with detailed information. The capacity dia-
gram is created in Capacity Supply and shows all paths, pre-constructed paths, bandwidths,
empty space for tailor-made requests, and defined TCRs including the maintenance windows.
Every change that impacts capacity on lines and in stations/nodes should be taken into ac-
count.
   To be able to share and publish information about the capacities and their update it is nec-
essary to establish the system-to-system data exchange by using the capacity product mes-
sages.
    Since the capacity messages do not exist in the existing TAF/TAP TSI schema, it is needed to
define new messages for sharing information about the available capacities between the sys-
tems (national and central).
       This section covers the positive capacity messages while the messages related to the
negative capacity were explained in the TCRs topic (see chapter 3.3.). A complete message
schema can be found in Annex 1 of this document.
   To manage the capacity products, new objects, elements and messages are proposed. In
addition, the existing document of the TAF/TAP TSI schema should be updated.
   The new objects that should be defined in the “ObjectType” element are as follows:
       •   Capacity band (BA)
       •   Pre-arranged path (PP)
       •   Catalogue path (CP)
       •   Capacity Model Object (CM)
       •   Capacity Needs Announcements (CN)
       •   TCR (TC)
       ▪
   The list of proposed new messages that will be created and implemented in the TAF/TAP TSI
schema, together with their description, are defined below:
         Summary
 Completeness:
IM      RNE
     - The Capacity Product messages were defined and incorporated into the JSG          TEG/SMO   TTR IT
       schema.
▪
 Open issues:                                         Reasons:
     - Requesting multi-annual products from RA
- The capacity process should be defined in more
     - Multi-annual RP use cases are missing
details by the Process Implementation group, spe-
cially process related to the multi-annual RP (in-
cluding all use cases) and process related to the
ad-hoc requests
 Next steps:
     - Define the use cases for the detailed multi-annual rolling planning processs
     - Definition of how the RAs will request multi-annual products
     - Check proposals within the TTR IT core team meeting
     - Define additional elements (objects) if necessary
 Dependency:
     - Feedback from the sub-group TTR process (RP use cases, feedback on prepared diagrams)
 (regarding the project, modules, other activities)
     -
 Implementation deadlines:
 -       TTR central: Implementation of the Capacity messages will be done by the end of Q1/2024
 -       National implementation: Since the messages were defined and published in the JSG schema and
handbook, IMs are invited to consider them to be implemented in their national tools
3.3.1. Capacity Model message
    To exchange data between the Applicants and IMs national tools and central ECMT (Euro-
pean Capacity Management Tool), the CapacityModel message is proposed to be used. This
message will serve for both, capacity needs announcements and capacity model data (traffic
volumes) exchange. IMs should be able to provide and update data about the traffic volumes for
the Capacity Model Objects (CMOs). Also, Applicants should be able to announce their capacity
needs, which will be considered by IMs. The frequency of sending the message depends on
needs of both sides.
3.3.2. Capacity Product Search message
The Capacity Product Search message shall be used to search for all kinds of capacity
positive and negative). The technical attributes for searching are contained in the Searching Cri-
teria of the message in the current TAF/TAP TSI schema, containing the parameters for each ob-
ject type (temporary capacity restriction, catalogue path, path, capacity model and capacity
needs announcements). The national application shall send the Capacity Product Search mes-
sage to search for the capacity that is in the focus of their needs, and the Capacity Product mes-
sage shall return the list of all capacities that fulfil the searching parameters.
3.3.3. Capacity Product message
The Capacity Product Message shall be used to return the result for the Capacity Product
Search Message. It contains information about the TCRs as negative capacity, information on
catalogue paths, paths, and capacity models as a positive capacity that matches the search cri-
teria provided in the Capacity Product Search Message.
Figure 5 – Capacity products message sequence
3.4. TCR Messages
    The Temporary Capacity Restrictions (TCRs) are necessary to keep the infrastructure and its
equipment in good condition (maintenance) and to allow infrastructure development in accord-
ance with market needs. TCRs refer to restrictions of the capacity of railway lines, for reasons
such as infrastructure works, including associated speed restrictions, axle load, train length,
traction, or structure gauge.
    The TCRs represent negative capacity on the network, and they are a capacity reduced fac-
tors that, if badly coordinated, decrease the stability and therefore the quality of timetables.
TCRs should be known in advance (even up to 36 months) and well planned in order to provide
high quality path offers. It is important to coordinate these TCRs at the international level, in-
clude Applicants in the process, and communicate unavailable capacity accordingly. Currently,
the communication between the national TCR systems (IMs, RUs) and RNE central TCR tool is
not possible because of the lack of the technical interface and TAF/TAP message structures for
data exchange. Data should be updating more frequently (nearly to daily basis) and because of
that technical interfaces for communication between central TCR tool and national TCRs sys-
tems are important.
   In the existing TAF/TAP TSI schema, the messages to manage TCRs do not exist. Therefore,
new messages were defined, and their structure was explained at the Sector Management
Office (SMO) and Joint Sector Group (JSG) meetings. The TCR messages were approved at both
meetings and shall be implemented into the TAF/TAP TSI schema.
    The list of proposed TCR messages that will be implemented in the TAF/TAP TSI schema,
together with their description, are defined below:
Summary
 Completeness:
     - The TCR messages were approved by TTR IT groups, TEG and SMO and they                 IM      RNE
       are ready to be implemented in the TCR Tool and national tools                      TEG/SMO   TTR IT
     - Some additional attributes might be added, to describe the TCR more pre-
       cise and in more detail (like information about the track that is affected by
       TCR)
 Open issues:                                       Reasons:
     - Additional attributes (e.g. a TCR on the
- To give more precise information on the TCR itself in
       specific track in the PLC or segment)
case there is more than one track line (line between
     - Some extension of the temporal expan-
stations)
       sion element could be made
- The extension of the XSD in sense of temporal expan-
 ▪
sion needed to import maintenance windows in future
 Next steps:
     - Update the proposed schema with additional elements in the sense of mesoscopic data usage
 Dependency: IT specification prepared according to the funding topic
     -
 Timeline:
     -
 Implementation deadlines: According to the funding deadline, it must be implemented by the end of
 2023, which will be later (feasible by the end of Q1/2024)
 -
 -
## 1.1.4      3.4.1. TCR Message
    The TCR import message shall be used by IMs to import TCRs from their national tool into
the TCR tool. The same message shall be used to update already created/imported TCRs.
<xs:element name="TCRMessage">
<xs:complexType>
<xs:sequence>
<xs:element ref="MessageHeader"/>
<xs:element ref="AdministrativeContactInformation"/>
<xs:element ref="TCRID"/>
<xs:element ref="CoordinatingIM" minOccurs="0"/>
<xs:element name="TCR" type="TCRType"/>
</xs:sequence>
</xs:complexType>
</xs:element>
Figure 6 – TCR Import Message
    The structure of this message contains following elements:
    MessageHeader element
    This element is a standard element that is used in all messages to identify the message it-
self (MessageReference), to give information about the sender (Sender) and the recipient
(Recipient) and to give some additional information like reference used by the sender (Sender-
Reference) and routing of the message to the correct application (MessageRoutingID).
   The information that will be provided in the “MessageReference” should be one of the val-
ues below, depends on the message that is sending:
       6500 TCRMessage
       6501 TCRResponseMessage
       6502 TCRCanceledMessage
   The message will be sent to the RNE, that means that Recipient is 3178, what is UIC of the
RNE.
An example of the message header implementation in the XML:
  <Example>
       <MessageHeader>
<MessageReference>
<MessageType>6500</MessageType>
<MessageTypeVersion>2.2.2</MessageTypeVersion>
<MessageIdentifier>UUID given by Common Interface</MessageIdenti-
fier>
<MessageDateTime>2020-04-05T09:30:47Z<MessageDateTime>
<MessageReference>
<Sender>0080</Sender>
<Recipient>3178</Recipient>
       <MessageHeader>
   </Example>
   AdministrativeContactInformation element
     The Administrative contact information element is used to give information which person
inside the IM company, that was created TCR, is responsible for managing the TCR. This is a per-
son who is TCR manager in the company or TCR manager for the specific region inside the coun-
try.
   TCRID element
    The TCRID mandatory element is an identifier of the TCR that is being sent by message. One
TCR per message will be sent. The structure type of the TCRID identifier is “CompositIdentifi-
erPlannedType”.
  The “TCRID” element is the unique identifier of the TCR object and is mandatory. This ele-
ment is a type of “CompositeIdentifierPlannedType” with the following information:
•   ObjectType – mandatory fixed value “TC” which defines TCR object
•   CompanyCode – mandatory numeric value (4 digits) in range 0000 to 9999 that
identifies the RU, IM or other company involved in the Rail Transport Chain
•   Core – is the main part of the identifier and is determent by the company that
creates it. It is a mandatory 12 characters alphanumeric string value.
•   Variant – shows a relationship between two identifiers referring to the same
business case. It is a mandatory 2 characters alphanumeric string value.
•   TimetableYear – refers to the timetable period in which the business will be car-
ried out. It is a mandatory numeric value (4 digits) in range 2012 to 2097.
•   StartDate – it is an optional date value that represents the start of the date in ef-
fect
Example
   In the case that the DB Netz sends TCR with the ID=12345, the TCRID should look like as fol-
lows (the “StartDate” value is not included):
TC – 0080 – 000000012345 – 00 – 2022
An example of implementation in the XML:
  <Example>
       <xs:TCRID>
<xs:ObjectType>TC</xs:ObjectType>
<xs:Company>0080</xs:Company>
<xs:Core>000000012345</xs:Core>
<xs:Variant>00</xs:Variant>
<xs:TimetableYear>2020</xs:TimetableYear>
<xs:StartDate>2019-12-08</xs:StartDate>
       </xs:TCRID>
   </Example>
   CoordinatingIM element
    This element is used to define which IM is responsible for the TCR and coordinates the pro-
cess between IMs. It is important specially in the countries with more than one IM, where IMs
are able to create TCRs for each other. This element is optional.
   TCR element
   The TCR element is the most important element of this message. This element is of type of
“TCRType” and it contains all the necessary information that describes the TCR object itself.
<xs:complexType name="TCRType">
 <xs:sequence>
       <xs:element ref="ReasonForRestriction"/>
       <xs:element name="Description" type="xs:string" minOccurs="0" />
       <xs:element ref="StartLocation" />
       <xs:element ref="EndLocation" />
       <xs:element name="Sections" type="SectionsType" minOccurs="0" />
       <xs:element ref="TCRDirection"/>
       <xs:element name="AffectedBorders" type="AffectedBordersType" minOccurs="0" />
       <xs:element name="AffectedIMs" type="AffectedIMsType" minOccurs="0" />
       <xs:element name="InvolvedICEs" type="InvolvedICEsType" minOccurs="0" />
       <xs:element name="TemporalExpansion" type="TemporalExpansionType" />
       <xs:element name="OperationalConsequenes" type="OperationalConsequencesType"/>
       <xs:element name="ProjectID" type="xs:string" minOccurs="0"/>
       <xs:element ref="TCRStatus"/>
       <xs:element name="LastUpdated" type="xs:dateTime"/>
       <xs:element name="AutomaticProcess" type="xs:boolean" minOccurs="0">
 </xs:sequence>
</xs:complexType>
   The “ReasonForRestriction” element gives an indication about the works regarding the
TCR. The following values are defined and can be used:
•   10 - Signal
•   20 - Switch
•   30 - Catenary
•   40 - Track/Rail
•   50 - Tunnel
•   60 - Bridge
•   70 - Miscellaneous
•   80 - Maintenance
•   90 - Other
   <xs:element name="ReasonForRestriction">
       <xs:simpleType>
<xs:restriction base="xs:token">
<xs:enumeration value="10"/>
<xs:enumeration value="20"/>
<xs:enumeration value="30"/>
<xs:enumeration value="40"/>
<xs:enumeration value="50"/>
<xs:enumeration value="60"/>
<xs:enumeration value="70"/>
<xs:enumeration value="80"/>
<xs:enumeration value="90"/>
       </xs:restriction>
     </xs:simpleType>
   </xs:element>
    “Description” element is used to give a brief description of the TCR. It is optional element
and can be used to make some more information related to the TCR. The type of this element is
<xs:string>.
    The “StartLocation” defines the beginning while the “EndLocation” defines the end loca-
tion of the TCR. Both fields are mandatory and only locations associated with the country of the
issuing IM are allowed. The location is described with the Country Code (CountryCodeISO) and
Location Primary Code.
<Example>
<CountryCodeISO>AT</CountryCodeISO>
<LocationPrimaryCode>1003</LocationPrimaryCode>
<PrimaryLocationName>Wien Hbf</PrimaryLocationName>
</Example>
    The “Sections” element defines the sections within where the TCR occurs. Multiple section
items can be defined. This field is optional. The value must match the section which is com-
puted from the fields “Location from” and “Location to”.
    The “TCRDirection” field is mandatory and defines which direction of the section is affected
by the TCR (bi-directional, a direction towards starting point of the location or direction towards
ending point of the location). The values that can be used are the following:
•   10 – Both direction
•   20 – End to start
•   30 – Start to end
     The “AffectedBorders” is an optional field used to define the location that is a border, in
case that TCR touches or is define in this location. That means if the selected “Location from” or
“Location to” entity is defined as a border station; this value should be set in the affected border
field as well.
    The “AffectedIMs” is an optional field used to involve the neighbouring IMs, that are af-
fected by the TCR, in the harmonisation process.
    The “InvolvedICEs” is an optional field used to involve the International Coordination Enti-
ties that are affected or should be involved in the TCR coordination.
    The “TemporalExpansion” is a mandatory field and it is used to define a date and time of
the TCR, and also temporal expansion type and duration of the TCR.
When defining the “TemporalExpansion”, the attribute that defines the expansion type of the
TCR should be chosen. There are two possibilities:
    •   Periodical - The characteristic of this event is described with a repeating pattern (e.g.
work activities happen each Saturday and Sunday from 02:00 to 04:15). For periodical
works, specific working days can be selected with checkboxes, where each checkbox
represents the beginning day of each work. In the case of the given example with works
on Saturday and Sunday from 02:00 to 04:15, the checkboxes Sat and Sun needs to be
ticked (not Sat, Sun and Mon). A help-text (tooltip) is displayed when hovering over the
label for working days and giving a brief description of the logic behind the temporal ex-
pansion of the TCR.
    •   Continuous - These events are characterized in a way that they occur non-stop during
the TCR (e.g. a complete closure of a track from 01.07.2017 to 01.09.2017).
Figure 7 - Temporal expansion structure of the TCR
   Depends on if the exact time of the TCR is known or unknown, the “PlannedCalendar” or
“RoughtDates” should be chosen.
   The structure of the “PlannedCalendar” is chosen when the exact time of the TCR is known
and its structure is as follows:
     For the “Continuous” TCRs, the “BitmapDays” element will be used in a case of the irregu-
larly validity days. In the case there is no irregularities, the “BitmapDays” can be avoid (not
used).
    For the “Periodical” TCRs it will be used to define all affected days. The weekly pattern will
be defined with the “WeeklyPatern” element that represents days in a week. The first character
represents Monday, the second Tuesday and so on.
    That means, for the periodical TCRs that occur every weekend from Friday to Sunday, the
BitmapDays value looks as follows:
       <BitmapDays>0000111</BitmapDays>
The “ValidityPeriod” element is mandatory and defines the start and end validity of the
TCRs. With these elements the “Date/Time from” and “Date/Time to” values in the TCR Tool will
be defined. The value of the element is DateTime.
    Each of the temporal expansion types is defined by “StartDate” and “EndDate” and also by
the “WeeklyInterval” in case of periodical and periodical continuous events.
<Example>
       <PlannedCalendar>
<BitmapDays>0010100001010000101000010100001</BitmapDays>
<ValidityPeriod>
<StartDateTime>2020-10-17T09:30:47Z</StartDateTime>
<EndDateTime>2020-11-17T09:30:47Z</EndDateTime>
</ValidityPeriod>
       </PlannedCalendar>
       <WeeklyPattern>0101000</WeeklyPattern>
       <WeeklyInterval>1</WeeklyInterval>
</Example>
   The “NumberOfSlots” and “AllocationStatus” elements are NOT related to the TCR Tool
and these elements will not be explained here. Since the same object will be used for the ca-
pacity products, these two elements are defined to avoid multiplication of the same object.
   The “OperationalConsequences” is a mandatory field to provide information regarding the
consequences of the TCR on the operations. This includes the impact on traffic, classification of
impact, traffic measures, necessary deviations and the incorporation of traffic measures in the
yearly timetable. The impact on traffic triggered by TCR is:
       •   Reduced track availability (LT – Line track; ST – Station track)
       •   Dimensional restrictions: Weight, Length, Profile
       •   Total closure
       •   Speed restrictions
       •   No catenary
       •   Affected traffic volume – a volume of trains affected by TCR (in percentage)
       ▪
The TCR classification is a mandatory field and classifies a TCR depends on its impact on the
traffic, as presented in the table:
Impact on traffic
Operator (Con-
Consecutive days       (estimated traffic cancelled, re-routed or        dition)
replaced by other modes of transport)
>=30 days
  Major impact       More or equal 30       More than 50% of the estimated traffic
AND
      TCR            consecutive days         volume on a railway line per day
>=50%
More than 7 and                                                     (>7 & <30) days
   High impact                              More than 30% of the estimated traffic             AND
less than 30 con-
       TCR                                    volume on a railway line per day
secutive days                                                      (>30% & <50%)
<=7 days
   Medium im-       7 consecutive days      More than 50% of the estimated traffic
AND
    pact TCR              or less             volume on a railway line per day
>=50%
  Minor impact                                                                                “null”
      TCR                                   More than 10% of the estimated traffic
unspecified3                                                            AND
volume on a railway line per day
>10% & <50%
    The traffic measures are defined by Cancellation, Re-routing, Replacement (Train/Bus) and
Estimated delay values. For each of these measures the type of trains that are affected could be
defined (freight, long distance, short distance, commuter). This field is optional.
    The “DeviationLocations” is and optional field that defines a location within the own net-
work, where the rail traffic shall be re-routed. The “DeviationBorders” is an optional field that
defines a border where the rail traffic shall be rerouted. Also, some additional comments re-
lated to the deviation selection could be done.
     The field “InYearlyTimetable” gives information on whether the TCR has been incorporated
in the annual timetable or not. The field is mandatory.
    The “ProjectID” is an optional field used by IMs to give information which their national pro-
ject is related to this TCR.
    The “TCRStatus” is an optional field used to define the status of the TCR. For the time being,
the TCR status is defined automatically by the TCR tool regarding the process and cannot be set
manually. Related to the simplification of the process, this field could be used to manually set
the status of the TCR.
    The “LastUpdate” is an optional field used to store information of the last TCR update.
## 1.1.5   3.4.2. TCRResponseMessage
    The message shall be used by the central tool as a response to the TCRMessage. It contains
the status and a report on data import with warnings and resolutions of these warnings to all re-
ceived data. The TCRResponseMessage provides an overview of data for all received infor-
mation at once. The message was defined with the main purpose of providing feedback on all
received data with just one message, including the resolution of possible errors. This message
contains information about the ID for each received TCR and its error status after check.
    In addition, the messages can be used nationally.
1.1.6   3.4.3. TCRCanceledMessage
    Using this message, IMs will be able to cancel the particular TCR that was sent to the TCR
tool. It is important to highlight that the TCR will not be permanently deleted from the TCR tool
database. The status of the TCR will be changed to “Canceled” and this TCR will not be editable
anymore.
<xs:element name="TCRCanceledMessage">
<xs:complexType>
<xs:sequence>
<xs:element ref="MessageHeader"/>
<xs:element ref="TCRID"/>
<xs:element name="Description" type="xs:string" minOccurs="0"/>
<xs:element ref="TypeOfInformation" minOccurs="0"/>
<xs:element ref="CoordinatingIM" minOccurs="0"/>
</xs:sequence>
</xs:complexType>
</xs:element>
   Only the IM who is the owner (creator / originator) of the TCR can cancel it. In a case of the
cancelation, the TCR owner will send this message with the TCRID information to specify which
TCR shall be cancelled.
    The detailed technical specification for the implementation of the TCR messages (technical
interface) on the national level can be found in the Annex 4 of this document. Description of the
Excel structure for TCR import is provided in the same annex as well.
Figure 8 - TCR ID element structure
## 1.1.7   3.4.4. CapacityProductSearchMessage
    To search for the TCRs, the “SearchCapacityProductMessage” shall be used. Using this
message, it is possible to search for “negative” and “positive” capacity product. The TCRs rep-
resent the “negative” capacity product, while capacity products like capacity bands, catalogue
paths and pre-arranged paths represent the “positive” capacity products.
    The “SearchCriterias” contains two optional criteria elements:
•   “TCRCriteria” – that is used to search for the TCRs
            •   “PositiveCapacityProductCriteria” – that is used to search all available products
capacity bands, catalogue and pre-arranged paths that can be used (free, re-
served or booked).
▪
Figure 9 - Search Capacity Product Message
    The TCR criterias or attributes that can be searched are as follows:
•   TCRIDSearch – searches for the specific TCR ID.
•   ReasonForRestrictionSearch – searches for the works regarding the TCR
•   SectionsSearch – the sections that shall be searched
•   TCRStatusSearch – searches for the status of the TCR. The currently defined sta-
tuses could be changed, depends on the process simplification
•   DateFromSearch – the beginning date from which TCRs will be searched
•   DateToSearch – the ending date to which TCRs will be searched
•   ReducedTrackAvailabilitySearch – search for TCRs with reduced track availability for
long or short distance trains
•   DimensionRestrictionSearch – search for the TCRs with dimension restriction
(weight, length, profile)
•   TotalClosureSearch – search for the TCRs with total closure impact on traffic
•   SpeedRestrictionSearch – search for the TCRs with speed restriction
•   NoCatenarySearch – search TCRs with diesel only availability
•   AffectedTrafficVolumeSearch – search TCRs that affects specific traffic volume
•   TrafficMeasuresSearch – search TCRs with cancellation, re-routing, replacement
and estimated delay measures
•   InvolvedRFCsSearch – search TCRs by involved RFCs
•   AffectedBorderSearch – search TCRs by affected borders
•   AffectedIMSearch – search TCRs by affected IMs
•   TCRClassificationSearch – search TCRs with a specific classification
The Positive capacity product criterias that can be searched are as follows:
•   IDSearch – searches for a specific capacity product by identifier
•   SectionsSearch – the sections that shall be searched
•   InvolvedRFCsSearch – search capacity products by involved RFCs
        •   CapacityManagerSearch – search capacity products per responsible capacity man-
ager
•   AffectedBorderSearch – search capacity products by affected borders
•   AffectedIMSearch – search capacity products by affected IMs
•   DateFromSearch – the beginning date from which capacity products will be
searched
•   DateToSearch – the ending date to which capacity products will be searched
•   ObjectTypeSearch – search capacity products per specific object type
## 1.1.8   3.4.5. CapacityProductMessage
   The “CapacityProductMessage” shall be used to return the result of the Search Capacity
Product Message.
Figure 10 - Capacity product message structure
    This message contains information related to the TCRs, Capacity bands, Catalogue and Pre-
arranged paths. Depends on which capacity product was searched, information about the prod-
ucts that where implied with the search shall be returned.
1.2 Capacity process and capacity products
    To be able to do the central and national implementation of the capacity products, it is nec-
essary to understand the process. The aim of this part of the documentation is to show the pro-
cess for the construction of the positive capacity products and describe the different use cases
that shall be supported by the central and national implementation. The scope of this part of the
documentation is to focus only the management (publication, modification) of the capacity
product.
    This topic is focusing on the detailed capacity model, where each product exists as a sepa-
rated object. For a higher-level capacity model, where only the volume of traffic is exchanged
among the IMs/ABs, a different approach, like sharing sheets and documents, can be applied.
   The definition of how to make requests on the capacity products, including eventually multi-
annual requests is not in the scope of this topic. Those can be found in the Path Request and
Path Management module.
Identifiers
    The usual structure of the TAF/TAP-TSI composite identifier is applied. As these objects are
only planning related, the start date is not applicable.
   •   ObjectType: provides a possibility for differentiation among the objects, in this case: Ca-
       pacity Band (BA), Pre-arranged Path (PP), Catalogue Path (CP), Capacity Model Object
       (CM), Capacity Needs Announcements (CN), TCR (TC)
   •   Company: identifies the company (IM/AB). Here, always the IM/AB identifier is used, even
       if the capacity is handled by an RFC
   •   Core: it is the main part of the identifier and is determent by the company that creates it
   •   Variant: it shows a relationship between two identifiers referring to the same business
       case
   •   TimetableYear: refers to the timetable period in which the business will be carried out
    Similar to the Path Management module, if an IM/AB is not able or not willing to generate its
own identifiers, the Central Tool will generate an identifier according to the structure above. For
example, if VPE sends a Capacity Band to the Central Tool without an identifier, it will define an
identifier like this:
   <Example>
       BA – 3032 – ******415689 – 00 – 2022
  </Example>
    When the national identifier is generated, the Central Tool is able to store and return that in
further communication.
Process
   Currently, two of the RNE applications (PCS and ECMT) work with these capacity products in
production, thus, their process was the baseline for the process definition.
    From a high-level point of view, the process shall look like the following simple workflow.
    Detailed processes are provided in the Annex 2.
    One process with the same process steps will be defined for all object types of positive ca-
pacity products. The information regarding their type is stored in the object type and in the type
of request. It’s the matter of the national implementation whether an IM/AB separates the pro-
cess steps (phases) also according to the objects. For example, in PCS the phases are defined
separately for CP and PP.
    When we are going to the more detailed process, we must differentiate two processes that
respect the same high-level aspect:
    •   Capacity product publication with RFC involved, as the current PP publication
    •   Capacity product publication without RFC, as the current CP publication
Impact of using the capacity products
    As said at the beginning of the document, it’s not in the scope of this part of the document to
describe the request management process on the capacity. However, it’s important to de-
scribe, what happens part of the capacity of the product is being used by an Applicant. Obvi-
ously, it will reduce the available capacity in the product for the other Applicants. When does
that happen? It depends on the process type and from the product point of view, it’s not even
important.
1.2.1.1 Calendar item aspects
    So far, we were dealing with the published calendar of the capacity product. When it comes
to using the product, we shall introduce the following calendar items:
    •   Remaining calendar: it means the calendar that is not yet requested
    •   Not reserved calendar: it means the calendar that is not yet reserved
   In some process types, these two comes hand in hand, like in Ad-Hoc or Rolling Planning
process types. There, the capacity reservation happens on a first-come-first-served logic. How-
ever, in the annual timetable process, there is a conflict resolution phase, where the Applicants
can pick the days from the “not reserved calendar” even if someone else requested those al-
ready.
    Please find here a demonstration of the different calendars.
Legend:
Published
Requested
Reserved
Not available
The situation, right after the publication:
Mo          7   14   21   28
Tu     1    8   15   22   29
    •   Published capacity: 1-7               We     2    9   16   23   30
    •   Remaining capacity: 1-7
    •   Not reserved capacity: 1-7            Th     3   10   17   24   31
Fr     4   11   18   25
Sa     5   12   19   26
Su     6   13   20   27
      The situation, when the request arrived in 1-4,7 and we are in the annual timetable process:
Mo                 7         14    21    28
Tu        1        8         15    22    29
      •       Published capacity: 1-7                   We        2        9         16    23    30
      •       Remaining capacity: 5-6
      •       Not reserved capacity: 1-7                Th        3        10        17    24    31
Fr        4        11        18    25
Sa        5        12        19    26
Su        6        13        20    27
The situation, when the capacity is reserved for 1-4,7:
Mo                  7        14    21    28
Tu         1        8        15    22    29
      •       Published capacity: 1-7
      •       Remaining capacity: 5-6              We         2        9        16    23    30
      •       Not reserved capacity: 5-6
Th         3       10        17    24    31
Fr         4       11        18    25
Sa         5       12        19    26
    The same applies when the                                                                         capacity is requested
or reserved partially from the                     Su         6       13        20    27              geographical aspect.
Let’s have an example, where the                                                                      above published
capacity is available from A to D.
A-B                              B-C                                              C-D
 Mo            7   14   21   28       Mo       7    14       21   28                  Mo         7    14   21   28
 Tu       1    8   15   22   29       Tu   1   8    15       22   29                  Tu    1    8    15   22   29
 We   2   9    16   23   30       We   2   9    16   23   30      We        2       9       16    23    30
 Th   3   10   17   24   31       Th   3   10   17   24   31      Th        3   10          17    24    31
 Fr   4   11   18   25            Fr   4   11   18   25           Fr        4   11          18    25
 Sa   5   12   19   26            Sa   5   12   19   26           Sa        5   12          19    26
 Su   6   13   20   27            Su   6   13   20   27           Su        6   13          20    27
However, the first request for 1-4,7 is valid only from A to C.
A-B                              B-C                                  C-D
 Mo       7    14   21   28       Mo       7    14   21   28      Mo                7       14    21    28
 Tu   1   8    15   22   29       Tu   1   8    15   22   29      Tu        1       8       15    22    29
 We   2   9    16   23   30       We   2   9    16   23   30      We        2       9       16    23    30
 Th   3   10   17   24   31       Th   3   10   17   24   31      Th        3   10          17    24    31
 Fr   4   11   18   25            Fr   4   11   18   25           Fr        4   11          18    25
 Sa   5   12   19   26            Sa   5   12   19   26           Sa        5   12          19    26
 Su   6   13   20   27            Su   6   13   20   27           Su        6   13          20    27
Then, the Capacity Manager reserves the capacity.
A-B                              B-C                                      C-D
 Mo       7    14   21   28       Mo       7    14   21   28           Mo               7        14    21    28
 Tu   1   8    15   22   29       Tu   1   8    15   22   29           Tu       1       8        15    22    29
 We   2   9    16   23   30       We   2   9    16   23   30           We       2       9        16    23    30
 Th   3   10   17   24   31       Th   3   10   17   24   31           Th       3       10       17    24    31
 Fr   4   11   18   25            Fr   4   11   18   25                Fr       4       11       18    25
 Sa   5   12   19   26            Sa   5   12   19   26                Sa       5       12       19    26
 Su     6   13   20   27           Su   6   13   20   27             Su   6   13   20   27
## 1.2.1.2 Slots in Capacity Bands
    This logic is not restricted to the calendar days. The same can be applied for the slots too.
Let’s check a capacity band and the same publication – request – reserve triangle.
   In this case, the formerly written calendar changes are valid only on the first slot of the ca-
pacity band, the rest remain untouched.
      The geographical aspect is also valid.
1.2.1.3 Returning the requested or reserved capacity
What happens, if the Applicant changes its mind during or after the request. From the capacity
product point of view, the timing is not important, but rather the impact and the way to follow
that.
    As you could see, the published calendar was never changed due to requests or reservation.
It means, when an Applicant decides not to request the former selected capacity (withdraws,
cancels, etc.), it shall be pasted back to the remaining and the not reserved capacity.
## 1.2.1.4 Calendar elements for positive capacity products in TTR IT
    The former explanation was important to understand the behaviour of the positive capacity
products and the different kind of calendar aspects. The Central Tool will always keep a regis-
try of the published, remaining and not reserved capacity. However, all of these calendars
won’t be presented in the schema.
   When IMs/ABs are working on the capacity products, like in the use cases, the published
calendar is propagated. Nationally, of course, a registry shall be kept by the IMs/ABs too for the
other calendars.
   Although, Applicants shall have the possibility to get information or even download the
available capacity. If an Applicant asks for the positive capacity products, the Central Tool shall
send back the objects with their not reserved capacity calendar.
## 1.3 Central implementation
    Since the capacity messages do not exist, it is needed to implement them into the TAF/TAP
TSI schema. After acceptance of the SMO and JSG, the messages will be sent to the ERA for the
final acceptance and implementation into the TAF/TAP TSI schema.
     A new TAF/TAP TSI schema that will be provided by ERA, should be implemented in the cen-
tral TTR IT framework.
Access Control
    The Central Tool provides access to the involved actors, but there are restrictions. Please
find below the summary of this description.
Elaboration
    The responsible IM, the affected IM(s) and the involved RFC(s) have always at least read-
only access to the product.
    The Capacity Manager (either the responsible IM or the RFC) has editing access to the prod-
uct.
    The affected IM has the option to set harmonization status (light) and place a comment on
the product.
Published
   All authenticated user has read-only access to the product.
    The Capacity Manager (either the responsible IM or the RFC) has the option to withdraw,
close or return capacity from the product.
   As an Applicant, the read-only access doesn’t mean equally requesting possibility. Even if a
product is published, the request can be placed only according to the defined timeline, also
matching the product’s partition (for an annual timetable or rolling planning).
Closed
   The responsible IM, the affected IM(s) and the involved RFC(s) have read-only access to the
product.
## 1.4 National implementation
    IMs/ABs on the national level have to implement the capacity product and TCR messages in
their national systems to exchange capacity data with the Central TTR IT Framework using the
interface.
## 1.4.1    3.7.1 Use case overview
    Regardless of the object types, the use cases are quite similar for each of them. That is why,
below you can find a general use case for an object, but later, in the use case details, the sepa-
rated explanation is provided for each object type. The same applies to the involvement of an
RFC. Thus, in the use case overview, we applied only the phrase “product”.
    Each use case will contain the explanation of how to do a particular action, but also how the
involved parties are notified by that.
   The use cases will focus on the different types (message, information, request, etc.) and we
consider the Path Information and Band Information elements as trivial. Only the relevant ele-
ments will be highlighted.
   Before publication
1.   Create a product with and without affecting a border
2.   Update product as work in progress
3.   Setting green light, harmonization accepted
4.   Setting red light, harmonization rejected or placing a comment
       5. Transfer a product
       6. Publication of a product
       7. Close product
   After publication
       8. Withdraw a product
       9. Return capacity from a product
       10. Modify a product (alteration)
       11. Close product
    The use case details contain information about the exchanged messages. The process de-
scriptions in the Annex 2 have them as well. Please note that the descriptions are focusing on
communication with the Central Tool. However, the same message exchange can be utilized for
bilateral communication among IMs/ABs only, without involving the Central Tool. In that case,
only the sender/recipient information shall be adjusted accordingly.
## 1.4.1.1 Create a product with and without affecting a border
    IMs/ABs have several possibilities when they create new capacity products. Please find here
the options:
   •   Type of the product. It can be CP, PP or BA
   •   The product is on an RFC line or on a non-RFC line
   •   The product affects another IM and requires international coordination
   •   The responsible IM remains the capacity manager or it transfers the capacity to an RFC
   We won’t describe all the possible combinations, but the document will give you the infor-
mation on how you should apply the different fields for your situation.
   Create CP on a non-RFC line without affecting neighbouring IM/AB
    ÖBB would like to create a CP from Linz Hbf to Wien Hbf. It’s a national section that is not
part of any RFC in this example. The IM/AB can rely on Capacity Product Coordination message
to create the capacity product.
Capacity Product Coordination message:
   •   Message header:
o Sender: 0081 (ÖBB)
o Recipient: 3178 (RNE)
   •   TOR: 9 – Catalogue Paths
   •   TOI: 30 – Create Dossier
    •   MS: 1 – Creation
    •   Identifier:
o CP – 0081 – **M-AMA12345 – 00 – 2022
    •   Capacity Manager: 0081 (ÖBB)
    As a result of this message, the Central Tool will create the new object and generate its own
internal identifier. That will be part of the Case Reference ID. To notify about this action the cre-
ator IM/AB, the Central Tool sends back a Receipt Confirmation message.
Receipt Confirmation message:
    •   Message header:
o Sender: 3178 (RNE)
o Recipient: 0081 (ÖBB)
    •   TOR: 9 – Catalogue Paths
    •   TOI: 30 – Create Dossier
    •   Identifiers:
o CR – 3178 – ******219456 – 00 – 2022
    •   Capacity Manager: 0081 (ÖBB)
    •   Related Reference
o Related Type: Capacity Product Coordination Message
o Related Identifier: CP – 0081 – **M-AMA12345 – 00 – 2022
    Create Capacity band on an RFC line without transferring the capacity, affecting the
    neighbour IM/AB
     VPE would like to create a BA from Ferencváros to Győr. It’s a national section, however,
they would like to inform about this object the neighbouring IM. It’s important that they can do
this. Similar to the TCRs and the TCR tool, each object that reaches the border, automatically
affects the neighbour. However, even if an object is national, an IM/AB can start international
coordination, because the object goes close enough to the border. In addition, this section lays
on an RFC line. Thus, VPE shall decide whether it transfers the capacity to RFC7. In this use
case, the capacity won’t be transferred to RFC7. It will be only marked as an involved RFC, just
like in the TCR tool.
Capacity Product Coordination message:
    •   Message header:
o Sender: 3032 (VPE)
o Recipient: 3178 (RNE)
    •   TOR: 7 – Capacity Bands
    •   TOI: 30 – Create Dossier
    •   MS: 1 – Creation
    •   Identifier:
o BA – 3032 – 2022321745*1 – 00 – 2022
   •   Affected IM: 0081 (ÖBB)
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 3032 (VPE)
As an answer, Central Tool sends back Receipt Confirmation message:
   •   Message header:
o Sender: 3178 (RNE)
o Recipient: 3032 (VPE)
   •   TOR: 7 – Capacity Bands
   •   TOI: 30 – Create Dossier
   •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022
   •   Related Reference
o Related Type: Capacity Product Coordination Message
o Related Identifier: BA – 3032 – 2022321745*1 – 00 – 2022
    Because ÖBB was marked as affected by VPE, the Central Tool will send a notification to
ÖBB. In case VPE doesn’t mark ÖBB as affected IM, but the product reaches the border of
VPE/ÖBB, the Central Tool will add this information automatically to the object and it will send
out the notification to ÖBB.
Capacity Product Coordination message:
   •   Message header:
o Sender: 3032 (VPE)
o Recipient: 0081 (ÖBB)
o Broker: 3178 (RNE)
   •   TOR: 7 – Capacity Bands
   •   TOI: 30 – Create Dossier
   •   MS: 07 – Create an offer
   •   Identifier:
o BA – 3032 – 2022321745*1 – 00 – 2022
   •   Affected IM: 0081 (ÖBB)
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 3032 (VPE)
   Create a Pre-arranged path on an RFC line and transferring the capacity, without affect-
   ing neighbouring IM/AB
   In this case, VPE would like to create a PP between Ferencváros and Győr, and it is handed
over to RFC7 from the capacity management point of view. The procedure is rather similar to the
previous one with slight changes. Now, VPE doesn’t feel the need to mark ÖBB as affected IM
and as it’s not touching the border, not even the Central Tool would mark them.
Capacity Product Coordination message:
    •   Message header:
o Sender: 3032 (VPE)
o Recipient: 3178 (RNE)
    •   TOR: 8 – Pre-arranged Paths
    •   TOI: 30 – Create Dossier
    •   MS: 1 – Creation
    •   Identifier:
o PP – 3032 – 2022321748*1 – 00 – 2022
    •   Involved RFCs: 1006 (RFC7)
    •   Capacity Manager: 1006 (RFC7)
    Before checking the confirmation from the Central Tool, what’s the difference compared to
the previous example?
    Involved RFC is RFC7 in both cases, but with the BA VPE kept its capacity manager position
and with this PP it transfers it to RFC7. This is in line with the current Pre-arranged Path process
in production. With the creation of the PaPs, the IMs/ABs give the capacity to the RFCs.
As an answer, Central Tool sends back Receipt Confirmation:
    •   Message header:
o Sender: 3178 (RNE)
o Recipient: 3032 (VPE)
    •   TOR: 7 – Capacity Bands
    •   TOI: 30 – Create Dossier
    •   Identifiers:
o CR – 3178 – ******123475 – 00 – 2022
    •   Related Reference
o Related Type: Capacity Product Coordination Message
o Related Identifier: PP – 3032 – 2022321748*1 – 00 – 2022
   There is no affected IM, meaning the Central Tool shall not send any further notification. The
RFC will get information about the action, but not via TAF TSI message as they are using the
Central Tool directly.
    Particularities with RFCs
    As a section on a line can belong to more RFCs, it’s possible to include more involved RFCs
in an object. It works the same with the TCRs in the TCR tool. However, the capacity manage-
ment shall be in the hand of one C-OSS Manager, meaning in one object only one RFC can be
nominated as Capacity Manager.
1.4.1.2 Update product as work in progress
     In this case, the IMs/ABs are updating their timetable (Path or Band information) in the Cen-
tral Tool. It will be demonstrated with two examples:
    •   VPE updates a BA which affects the border that triggers a notification to ÖBB
    •   RFC7 updates a PP that triggers a notification to VPE
    If there is no RFC – IM/AB relation or affected IM, an update won’t trigger a special notifica-
tion to other parties.
    IM/AB updates product with affected border
    VPE sends an update on the BA – 3032 – 2022321745*1 – 00 – 2022. As ÖBB marked as af-
fected IM, the Central Tool will broadcast the update information. As the object has been al-
ready created in the Central Tool and the identifier has been communicated to VPE, it shall in-
clude this identifier in the update message.
Capacity Product Coordination message:
    •   Message header:
o Sender: 3032 (VPE)
o Recipient: 3178 (RNE)
    •   TOR: 7 – Capacity Bands
    •   TOI: 01 – Harmonisation - in process
    •   MS: 1 - Creation
    •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 – 2022
    •   Affected IM: 0081 (ÖBB)
    •   Involved RFCs: 1006 (RFC7)
    •   Capacity Manager: 3032 (VPE)
As an answer, Central Tool sends back Receipt Confirmation:
    •   Message header:
o Sender: 3178 (RNE)
o Recipient: 3032 (VPE)
    •   TOR: 7 – Capacity Bands
    •   TOI: 01 – Harmonisation - in process
    •   Related Reference
o Related Type: Capacity Product Coordination Message
o Related Identifier: CR – 3178 – ******145876 – 00 – 2022; BA – 3032 –
2022321745*1 – 00 – 2022
ÖBB will receive Capacity Product Coordination message as a notification:
    •   Message header:
o Sender: 3032 (VPE)
o Recipient: 0081 (ÖBB)
o Broker: 3178 (RNE)
   •   TOR: 7 – Capacity Bands
   •   TOI: 01 – Harmonisation - in process
   •   MS: 1 - Creation
   •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 – 2022
   •   Affected IM: 0081 (ÖBB)
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 3032 (VPE)
   RFC updates product
    The object is still in the Elaboration phase and it allows the RFC (as Capacity Manager) to
update its content. The update itself happens on the GUI of the Central Tool, however, notifica-
tion shall be sent out to the IMs/ABs.
Capacity Product Coordination message:
   •   Message header:
o Sender: 1006 (RFC7)
o Recipient: 3032 (VPE)
o Broker: 3178 (RNE)
   •   TOR: 8 – Pre-arranged Paths
   •   TOI: 01 – Harmonisation - in process
   •   MS: 1 – Creation
   •   Identifiers:
o CR – 3178 – ******123475 – 00 – 2022; PP – 3032 – 2022321748*1 – 00 – 2022
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 1006 (RFC7)
## 1.4.1.3 Setting green light, harmonization accepted
    For products handled by RFCs, it’s the RFC who sets the light (harmonisation status). The
explanation is simple, the capacity is already harmonized among the IMs/ABs when the RFC
gets it. However, in our case, we shall cover the process of the whole capacity harmonization.
Therefore, the IMs/ABs involved in the international coordination will set their lights to green.
Conclusion: always the Capacity Manager and the Affected IMs must set light.
    Notice: if the capacity has been already transferred to the RFC and the RFC sets green light,
the IM/AB will get a notification via Capacity Product Coordination Message.
  To finish the harmonization process, the green light is necessary from each involved IM/AB,
meaning also the affected ones.
ÖBB can set green light with Capacity Product Coordination Message:
   •   Message header:
o Sender: 0081 (ÖBB)
o Recipient: 3178 (RNE)
   •   TOR: 7 – Capacity Bands
   •   TOI: 02 – Harmonisation - accepted
   •   MS: 1 - Creation
   •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 – 2022
   •   Affected IM: 0081 (ÖBB)
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 3032 (VPE)
   ▪
As an answer, Central Tool sends back Receipt Confirmation:
   •   Message header:
o Sender: 3178 (RNE)
o Recipient: 0081 (ÖBB)
   •   TOR: 7 – Capacity Bands
   •   TOI: 02 – Harmonisation - accepted
   •   Related Reference
o Related Type: Capacity Product Coordination Message
o Related Identifier:
▪ CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 –
2022
▪
VPE gets the notification about ÖBB’s action with a Capacity Product Coordination Message:
   •   Message header:
o Sender: 0081 (ÖBB)
o Recipient: 3032 (ÖBB)
o Broker: 3178 (RNE)
   •   TOR: 7 – Capacity Bands
   •   TOI: 02 – Harmonisation - accepted
   •   MS: 1 - Creation
   •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 – 2022
   •   Affected IM: 0081 (ÖBB)
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 3032 (VPE)
## 1.4.1.4 Setting red light, harmonization rejected or placing a comment
   The procedure is the same as the green light, with TOI: 03 – Harmonisation – rejected.
    In addition, a mandatory comment shall be pasted in the free text field on the message level
with the reason for rejection.
   When an affected IM would like to place only comment on the product, the same procedure
shall be applied with TOI: 01 – Harmonisation – in process.
## 1.4.1.5 Transfer a product
    Transferring product or actually capacity can happen between an IM/AB and an Interna-
tional Coordination Entity, in our case, an RFC. It means the change of the company code in the
Capacity Manager field. At the end of the transfer, the company whose ID is in the Capacity
Manager field will be responsible for handling the product. The transfer itself can happen any-
time during the Elaboration phase.
    From IM to RFC
    In our example, there is already a BA on an RFC line, but still in the hand of VPE. If they de-
cide to transfer this BA to RFC7, VPE can do it with a Capacity Product Coordination Message.
Capacity Product Coordination Message:
    •   Message header:
o Sender: 3032 (VPE)
o Recipient: 3178 (RNE)
    •   TOR: 7 – Capacity Bands
    •   TOI: XX – Capacity transfer
    •   MS: 1 - Creation
    •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 – 2022
    •   Affected IM: 0081 (ÖBB)
    •   Involved RFCs: 1006 (RFC7)
    •   Capacity Manager: 1006 (RFC7)
    ▪
As an answer, Central Tool sends back Receipt Confirmation:
    •   Message header:
o Sender: 3178 (RNE)
o Recipient: 3032 (VPE)
    •   TOR: 7 – Capacity Bands
    •   TOI: XX – Capacity transfer
    •   Related Reference
           o   Related Type: Capacity Product Coordination Message
o   Related Identifier: CR – 3178 – ******145876 – 00 – 2022; BA – 3032 –
2022321745*1 – 00 – 2022
ÖBB will receive Capacity Product Coordination message as a notification:
   •   Message header:
o Sender: 3032 (VPE)
o Recipient: 0081 (ÖBB)
o Broker: 3178 (RNE)
   •   TOR: 7 – Capacity Bands
   •   TOI: XX – Capacity transfer
   •   MS: 1 - Creation
   •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 – 2022
   •   Affected IM: 0081 (ÖBB)
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 1006 (RFC7)
   From RFC to IM
     Transferring the capacity can happen vice versa and, in this case, the RFC will transfer the
capacity to the IM. Of course, the RFC will do this on the GUI of the Central Tool, but the IM/AB
will be notified. In our example, there is a PP that is a good candidate for this showcase.
Capacity Product Coordination Message sent to VPE:
   •   Message header:
o Sender: 1006 (RFC7)
o Recipient: 3032 (VPE)
o Broker: 3178 (RNE)
   •   TOR: 8 – Pre-arranged Paths
   •   TOI: XX – Capacity transfer
   •   MS: 1 – Creation
   •   Identifier:
o CR – 3178 – ******123475 – 00 – 2022; PP – 3032 – 2022321748*1 – 00 – 2022
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 3032 (VPE)
## 1.4.1.6 Publication of a product
    In a previous use case (setting green light) we wrote that the Capacity Manager and the Af-
fected IMs must set green light. Based on this, the publication is possible only for the Capacity
Manager of a product. Of course, if the Capacity Manager is an RFC, the IM/AB will get notifica-
tion about the publication.
   Publication by an IM/AB
   As VPE is the Capacity Manager for BA – 3032 – 2022321745*1 – 00 – 2022, it’s possible for
them to publish it. When there is an affected IM, the Central Tool will inform them about this
change.
VPE sends Capacity Product Details Message to publish the band:
   •   Message header:
o Sender: 3032 (VPE)
o Recipient: 3178 (RNE)
   •   TOR: 7 – Capacity Bands
   •   TOI: XX – Publish capacity
   •   MS: 1 - Creation
   •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 – 2022
   •   Affected IM: 0081 (ÖBB)
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 3032 (VPE)
As an answer, Central Tool sends back Receipt Confirmation:
   •   Message header:
o Sender: 3178 (RNE)
o Recipient: 3032 (VPE)
   •   TOR: 7 – Capacity Bands
   •   TOI: XX – Publish capacity
   •   Related Reference
o Related Type: Capacity Product Details Message
o Related Identifier: CR – 3178 – ******145876 – 00 – 2022; BA – 3032 –
2022321745*1 – 00 – 2022
ÖBB gets Capacity Product Details Message as a notification:
   •   Message header:
o Sender: 3032 (VPE)
o Recipient: 0081 (ÖBB)
o Broker: 3178 (RNE)
   •   TOR: 7 – Capacity Bands
   •   TOI: XX – Publish capacity
   •   MS: 1 - Creation
   •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 – 2022
   •   Affected IM: 0081 (ÖBB)
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 3032 (VPE)
   Publication by an RFC
   As today, Pre-arranged Paths are published by the RFCs. We have one in our example for
RFC7 and it’s possible for him to publish. The C-OSS Manager user would this on the GUI of the
Central Tool, but the IMs/ABs involved will get the notification.
VPE gets Capacity Product Details Message as notification
   •   Message header:
o Sender: 1006 (RFC7)
o Recipient: 3032 (VPE)
o Broker: 3178 (RNE)
   •   TOR: 8 – Pre-arranged Paths
   •   TOI: XX – Publish capacity
   •   MS: 1 – Creation
   •   Identifiers:
o CR – 3178 – ******123475 – 00 – 2022; PP – 3032 – 2022321748*1 – 00 – 2022
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 1006 (RFC7)
## 1.4.1.7 Close product before publication
    As for any dossier, the Central Tool shall support the closure of a product. We are still be-
fore the publication, meaning the closure is possible at any time for the Capacity Manager com-
pany. The Capacity Manager can be either an IM/AB or an RFC, depending on the former set-
tings.
   Closed by an IM/AB
VPE shall send Capacity Product Coordination message to close their band:
   •   Message header:
o Sender: 3032 (VPE)
o Recipient: 3178 (RNE)
   •   TOR: 7 – Capacity Bands
   •   TOI: 31 – Close dossier
   •   MS: 1 - Creation
   •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 – 2022
   •   Affected IM: 0081 (ÖBB)
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 3032 (VPE)
As an answer, Central Tool sends back Receipt Confirmation:
   •   Message header:
o Sender: 3178 (RNE)
o Recipient: 3032 (VPE)
   •   TOR: 7 – Capacity Bands
   •   TOI: 31 – Close dossier
   •   Related Reference
o Related Type: Capacity Product Coordination Message
o Related Identifier: CR – 3178 – ******145876 – 00 – 2022; BA – 3032 –
2022321745*1 – 00 – 2022
ÖBB will receive Capacity Product Coordination message as a notification:
   •   Message header:
o Sender: 3032 (VPE)
o Recipient: 0081 (ÖBB)
o Broker: 3178 (RNE)
   •   TOR: 7 – Capacity Bands
   •   TOI: 31 – Close dossier
   •   MS: 1 - Creation
   •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 – 2022
   •   Affected IM: 0081 (ÖBB)
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 3032 (VPE)
   Closed by an RFC
    When an RFC, in our case RFC7, decides to close a capacity product, he would do it on the
GUI of the Central Tool. In this case, the involved IMs/ABs will be notified. Let’s take the exam-
ple when RFC7 would close his Pre-arranged Paths.
VPE gets Capacity Product Coordination Message as notification
   •   Message header:
o Sender: 1006 (RFC7)
o Recipient: 3032 (VPE)
o Broker: 3178 (RNE)
   •   TOR: 8 – Pre-arranged Paths
   •   TOI: 31 – Close dossier
   •   MS: 1 – Creation
   •   Identifiers:
o CR – 3178 – ******123475 – 00 – 2022; PP – 3032 – 2022321748*1 – 00 – 2022
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 1006 (RFC7)
## 1.4.1.8 Withdrawal a product
    Once a product becomes obsolete, there should be an option for the Capacity Manager to
withdraw that from the offer. This is true; however, it shall match certain pre-conditions:
   •   The capacity product is published: if it’s not the case, there is nothing to worry about,
       because the product is not yet public. If the Capacity Manager wants to get rid of this
       product, it can use the close product option.
   •   The capacity product is not used by anyone (Applicant): if the product is already used by
       someone, e.g. requested or even reserved, the withdrawal is not possible. The option of
       removing the rest of the capacity from the offer is still valid, but for that, please check the
       capacity return use case.
   •   Withdrawal is possible only for Capacity Manager
   As a result of the withdrawal, the capacity product will go back to the Elaboration phase.
   Withdrawal by an IM/AB
   VPE shall send Capacity Product Not Available message to withdraw their band:
   •   Message header:
o Sender: 3032 (VPE)
o Recipient: 3178 (RNE)
   •   TOR: 7 – Capacity Bands
   •   TOI: 29 - Withdrawal
   •   MS: 1 - Creation
   •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 – 2022
   •   Affected IM: 0081 (ÖBB)
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 3032 (VPE)
As an answer, Central Tool sends back Receipt Confirmation:
   •   Message header:
o Sender: 3178 (RNE)
o Recipient: 3032 (VPE)
   •   TOR: 7 – Capacity Bands
   •   TOI: 29 - Withdrawal
   •   Related Reference
o Related Type: Capacity Product Not Available Message
o Related Identifier: CR – 3178 – ******145876 – 00 – 2022; BA – 3032 –
2022321745*1 – 00 – 2022
ÖBB will receive Capacity Product Not Available message as a notification:
   •   Message header:
o Sender: 3032 (VPE)
o Recipient: 0081 (ÖBB)
o Broker: 3178 (RNE)
   •   TOR: 7 – Capacity Bands
   •   TOI: 29 - Withdrawal
   •   MS: 1 - Creation
    •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 – 2022
    •   Affected IM: 0081 (ÖBB)
    •   Involved RFCs: 1006 (RFC7)
    •   Capacity Manager: 3032 (VPE)
    Withdrawal by an RFC
    When an RFC, in our case RFC7, decides to withdraw a capacity product, he would do it on
the GUI of the Central Tool. VPE gets Capacity Product Not Available Message as notification
that RFC7 has withdrawn a PP.
    •   Message header:
o Sender: 1006 (RFC7)
o Recipient: 3032 (VPE)
o Broker: 3178 (RNE)
    •   TOR: 8 – Pre-arranged Paths
    •   TOI: 29 – Withdrawal
    •   MS: 1 – Creation
    •   Identifiers:
o CR – 3178 – ******123475 – 00 – 2022; PP – 3032 – 2022321748*1 – 00 – 2022
    •   Involved RFCs: 1006 (RFC7)
    •   Capacity Manager: 1006 (RFC7)
## 1.4.1.9 Return capacity from a product
    It was already mentioned partially under the Withdrawn use case. When a product is pub-
lished and also used by someone, e.g. requested or even reserved, but the Capacity Manager
would like to reduce the rest of the capacity, this procedure should be used.
   Notice: according to the current RFC process, inside a given timeframe, usually inside 30
days window, even the Central Tool can return the capacity and then the IMs/ABs can use that
capacity for their ad-hoc business.
    As a result, the product will remain in the Published phase, but with reduced capacity (the
published calendar is updated). The returned capacity becomes free and the IMs/ABs can in-
clude their planning for other processes.
   Depending on the percentage of the return, it can be either full or partial. Even if this can be
applied for Capacity Bands too, in order to avoid too much additional TOIs, we use here 32 –
Path cancelled full or 33 – Path cancelled partially.
   Notice: even the full return is available, it doesn’t make much sense. If that’s the case, it’s
recommended to use the withdraw option.
   What can we return?
    If there were only Pre-arranged Paths and Catalogue Paths, the answer would be simple:
calendar days. However, this procedure applies for the Capacity Bands too, where the Capacity
Manager can reduce the available slots too.
   Returned by an IM/AB
VPE shall send Capacity Product Not Available message to return the capacity in their band:
   •   Message header:
o Sender: 3032 (VPE)
o Recipient: 3178 (RNE)
   •   TOR: 7 – Capacity Bands
   •   TOI: 32 – Path cancelled full/33 – Path cancelled partially
   •   MS: 1 - Creation
   •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 – 2022
   •   Affected IM: 0081 (ÖBB)
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 3032 (VPE)
As an answer, Central Tool sends back Receipt Confirmation:
   •   Message header:
o Sender: 3178 (RNE)
o Recipient: 3032 (VPE)
   •   TOR: 7 – Capacity Bands
   •   TOI: 32 – Path cancelled full/33 – Path cancelled partially
   •   Related Reference
o Related Type: Capacity Product Not Available Message
o Related Identifier: CR – 3178 – ******145876 – 00 – 2022; BA – 3032 –
2022321745*1 – 00 – 2022
ÖBB will receive Capacity Product Not Available message as a notification:
   •   Message header:
o Sender: 3032 (VPE)
o Recipient: 0081 (ÖBB)
o Broker: 3178 (RNE)
   •   TOR: 7 – Capacity Bands
   •   TOI: 32 – Path cancelled full/33 – Path cancelled partially
   •   MS: 1 - Creation
   •   Identifiers:
o CR – 3178 – ******145876 – 00 – 2022; BA – 3032 – 2022321745*1 – 00 – 2022
   •   Affected IM: 0081 (ÖBB)
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 3032 (VPE)
   Returned by an RFC
   VPE gets Capacity Product Not Available Message as notification that RFC7 has returned
capacity from a PP via the GUI.
   •   Message header:
o Sender: 1006 (RFC7)
o Recipient: 3032 (VPE)
o Broker: 3178 (RNE)
   •   TOR: 8 – Pre-arranged Paths
   •   TOI: 32 – Path cancelled full/33 – Path cancelled partially
   •   MS: 1 – Creation
   •   Identifiers:
o CR – 3178 – ******123475 – 00 – 2022; PP – 3032 – 2022321748*1 – 00 – 2022
   •   Involved RFCs: 1006 (RFC7)
   •   Capacity Manager: 1006 (RFC7)
## 1.4.1.10
## 1.4.1.11 Modified a product (alteration)
    Alteration process on capacity products is not available. If there is a need to apply a change
for a certain part of a capacity product, the following approach shall be followed:
   •   Withdraw the product, if possible, do the update and publish again
   •   If the withdrawal is not possible, then return the capacity from the product and publish
       new capacity with the updated information.
   ▪
   ▪
## 1.4.1.12 Close product after publication
   The procedure and the messages are the same as for the closure before publication, how-
ever, the same pre-conditions are valid as for the withdrawal.
   Capacity Requests (X-11 to X+12)
At X-11, all the capacity which were prepared during the Advanced planning phase are pub-
lished as the capacity products. These published capacity products are used by Applicants to
requests their capacity needs for the annual timetable, rolling planning and ad hoc (including
short-term ad hoc) traffic.
## 1.5 4.1. Annual requests
Capacity requests for the annual timetable, including the examples are provided in the chapter
## 5.4. Path Management module (under the national implementation).
According to the TTR Process, a new path request deadline is moved to X-8.5 (the initial path re-
quest deadline was at X-8).
## 1.6 4.2. Rolling Planning requests
The Rolling Planning requests can be placed at any time by respecting the deadline between M-
4 to M-1 before the first day of operation.
Description of the rolling planning requests process and especially multi-annual requests shall
be provided in the separate specification document.
## 1.7 4.3. Ad hoc and short-term ad hoc requests
The explanation of the short-term ad hoc concept requests is roughly described in the chapter
## 5.8. Capacity Broker.
This chapter describes the concept itself but the detailed description shall be provided in a sep-
arate specification document (will be provided as an annex to this document).
    Modules (Microservices)
    TTR IT Landscape modules cover all the functionalities of business processes. In figure 1,
the basic overview of data exchange between modules is done. Communication and data ex-
change between the modules will be explained in more details in each of the modules’ topic.
The more detailed description of the data flow between the modules can be found in Annex 3 of
this document.
Figure 11 - Basic data flow between modules
1.8 5.1. Messaging module
     The Messaging module is the main module to ensure and establish communication between
Central TTR IT framework (RNE central systems) and external systems of IMs and applicants and
is the single point of connection between the systems. The module is completely based on the
TAF/TAP TSI messages exchange.
Figure 12 - Message Transfer based on Configurable Protocols and Message Formats
  National/stakeholders’ systems will communicate with the Central TTR IT Framework with
TAF/TAP messages (extended with Sector messages like PathCoordinationMessage) through the
Common Interface of the Messaging module. IMs have a possibility to use the Messaging mod-
ule for communication with their RUs or other IMs.
  The Common Interface (CI) will be used in the Messaging Module due to its functionality of
“reliable messaging”, in order to be able to react appropriately to the communication break-
down between the systems. The Common Interface is already productively intensively used in
real-real time messaging (especially in RNE TIS) and has been proven as a reliable system for
messaging by offering the possibilities of storing and resending the messages that failed in the
delivery.
  The communication will be done similarly as today between national systems and RNE PCS
system.
  The CI is a technical tool that supports the interoperable exchange of messages and is a part
of the Common Components System (CCS).
  CI can be locally installed in customers’ datacentres, it is peer to peer application (a message
transformation middleware), which does the transformation from legacy format of messages
into common or shared metadata format (XML) and vice versa. CI can also exchange other cus-
tomer-specific messages if the conditions concerning the message structure are met.
  Messages are sent and received through an open message queue, which is the interface be-
tween the Translation & Validation layer of the Common Interface and the Security & Transport
layer of the Common Interface. The Security and Transport layer manages the delivery and re-
ceipt of messages to and from the public network side of the Message Queue. The Translation
and Validation layer and API layer manage the receipt of data from and delivery of data to the
systems in use on the internal side of the Message Queue.
Figure 13 - Functional structure of the CI
## 1.8.1    5.1.1. Centralized master data
▪
  Access to the Master data is not needed since the Messaging module is used to support a
message exchange between the modules of the Central TTR IT framework and external (stake-
holders) systems.
Minimal requirements
  Using the version 2.1 of the Common interface (CI).
Additional requirements
  Support a bilateral communication (data exchange) between IM and RU partners without con-
sulting a central system.
## 1.8.2   5.1.2. Integration options
  All the communication between the Central TTR IT framework and national systems (and vice
versa) should be sent through the Messaging module and messaging must be established within
the TAF/TAP TSI.
Minimal requirements
Object updates
  All objects defined in the modules
Schema and message definition
  All TAF/TAP TSI based messages defined in the modules
Endpoint and communication
  Communication between all modules inside the central TTR IT framework and national sys-
tems
Additional requirements
  Ensure the communication between partners on the bilateral bases (exchanging TAF/TAP TSI
messages or customer-specific messages) and support them to not create different interfaces
to establish communication with each stakeholder. The stakeholders may use one point of con-
nection to communicate with others.
## 1.8.3   5.1.3. Connection to other modules
Minimal requirements
  Support message exchange between all modules and between central TTR IT framework and
national systems in all directions
Additional requirements
  Support exchange of the customer specific messages (bilateral communication between
partners).
## 1.8.4   5.1.4. Central implementation
   There is no need for any additional development of the CI to support the first phase of the TTR
IT Landscape implementation in the sense of the functionality.
  The settings in the RNE CI should be made for the companies that still did not establish a con-
nection to CI.
  The CI configuration should be simplified by implementing the wizard for the “step-by-step”
procedure from Remote LI, via Heartbeat to Outbound Routing configuration. The idea is to dra-
matically speed up the process of the configuration, especially for the newcomers. Also, the
guidance for the users should be provided, which will reduce the configuration errors which in
the most cases cause major delays in CI to CI communication establishing. The details about
the proposal of this “step-by-step” wizard development is described in the Annex 5.
  Another development to improve the CI is related to development of APIs for Monitoring. Cur-
rently, there is possibility to integrate CI in monitoring tools via SNMP events which can be
broadcasted from CI. However, the for the applications which have the possibility of seamless
integration (which is now state-of-the-art), it would be useful that CI provides an API which can
be used on demand for, at least, querying of message validation and broadcasting status. This is
just a minimal set of API methods that would help a lot in the integration of CI in the application
landscape. It can be implemented as REST or SOAP. Details about the proposal for the API (for
monitoring) development, is described in the Annex 6.
  Additional possibility, that could be considered is development in CI to be able to exchange
data using json. Such a specification shall be prepared after implementation of the minimum
TTR IT Landscape requirements for the TT2025 (described in this document).
▪
## 1.8.5   5.1.5. National implementation
  To start communicating and exchange messages with the central TTR IT framework, some
prerequisite must be done. These prerequisites include set up of the local interface (LI) and es-
tablishment of the heart bit between the systems. There are documents that explain in detail
how to install and set up the CI.
▪
## 1.9 5.2. BigData module
  The BigData tool was developed as a collaboration tool for topology visualization and handling
topology modifications on the map. The BigData contains railway network topology and con-
tains specialized views of that topology adapted to each RNE’s system.
   The BigData tool synchronizes data with CRD (Central Reference File Database) and contains
the whole set of information as the CRD database. The CRD as a centralised database that
stores Location codes and Company codes required by European regulation and makes them
available to users. The CRD introduces the concept of “Primary locations” and “Subsidiary loca-
tions”. In addition, in the BigData the segments and sections data are created.
  Considering some international Rail Topo Models (e.g., RINF, IRS 30100), we can say that the
BigData is based on the macro level infrastructure data. It is considering the implementation of
the RINF database and with this data, the BigData database will be covered the meso level net-
work. For the future, implementing a meso level network model is necessary. A new version of
the tool with synchronized RINF data will be called Railway Infrastructure System (RIS).
   The general requirements to the necessary infrastructure data from TTR’s point of view (de-
fined by Core Group 6) are the following :
•   Segment
o Identification – PLCs of the connected nodes with their country codes
               o    Distance – needed for the presentation purposes in the capacity model and
capacity supply
o    Validity
o    Line
o    Number of tracks
▪
•   Track
o   Identifier
o   Validity
o   Speed
o   Electric system
o   Train control system
o   Max. axle load
o   Gauge
o   Length
▪
## 1.9.1   5.2.1. Centralized master data
 As the first step of the TTR IT Landscape implementation, the RNE BigData should be the
Master data for all modules of the central TTR IT framework.
  It is important that every module uses the same infrastructure data.
 Each module will have its own database to store transactional (raw) data created inside the
module.
Minimal requirements
  The latest version of the BigData tool, with the completely integrated CRD as a data source for
ground topology, will be used as a minimal requirement and the first step for the BigData mod-
ule implementation.
  IMs and ABs continuously update data in the CRD by providing the latest infrastructure infor-
mation. It is necessary that IMs/ABs and CRD database has the same infrastructure data to
avoid “non-existence” or wrong data presentation.
Additional requirements
  For the second step and the whole BigData module implementation, additional information
on the tracks and locations should be provided. This additional information includes the data
related to the electricity, number and type of tracks (single, double, fast track for passenger,
tracks for freight, etc.), line category, etc. The meso level network model should be defined and
implemented.
  To fulfil this requirement, BigData database will be extended and synchronized with the RINF
database or provided infrastructure data from the national systems in a standardized way, to
describe the infrastructure for upcoming timetable years.
## 1.9.2   5.2.2. Integration options
  The frequent synchronization of the company and geo-topology data with the CRD database is
necessary to keep data up to date. Companies deliver their infrastructure data to the CRD data-
base, and this data is synchronized with the BigData database and will be reused by modules of
the central TTR IT framework.
Figure 14 - Topology data synchronization
Minimal requirements
Object updates
  Synchronizing the data using the views exposed through database synonyms. The following
objects are synchronized: Country, Company, Location (Primary location, Subsidiary location).
Schema and message definition
  To provide master data for every module, the views created in the BigData database will be
used.
Endpoint and communication
  TCR Tool (TCR)
        ➢ BigData -> TCR Tool (input data) – master topology data like Company, Locations,
Segments, Sections, Layers
➢ TCR Tool -> BigData (output data) - reference database views as TCR exports syno-
nyms
▪
  Path Management (PM)
➢ BigData -> (input data) – master topology data like Company, Locations, Segments,
Sections, Layers
▪
  Path Request Management (PRM)
➢ BigData -> TCR Tool (input data) – master topology data like Company, Locations,
Segments, Sections, Layers
## 1.9.3   5.2.3. Connection to other modules
  Minimal requirements
  The BigData will exchange infrastructure data with the following modules: Path Management,
Path Request Management, TCR
  Additional requirements
 Provide a view on the topology data for external systems in case of stakeholder’s request.
Data shall be provided via the Messaging module.
1.10 5.3. Capacity Needs Announcements (CNA)
  It is foreseen that RUs should indicate and expressed their needs about the trains in the next
timetable year.
  Currently, there is no internationally used application for this purpose. The intention was to
use the "Capacity Wish" form which is currently in use (Annex 8 of this document). RUs will indi-
cate the volume, length range, weight range, types of locomotives in use, and a rough idea of a
timetable, calendar and schedule.
 Related to decision from the FTE members, data will be imported directly to the Capacity Hub
module (ECMT) using the CNA Excel file structure (see Annex 8).
  There will not be developed as a separate module for the Applicants.
## 1.10.1 5.3.1. Centralized master data
  It is necessary to have access to the Master data for communication with IMs in order to be
able to indicate the operation points in the "Capacity Wish” form. For Master data, the overview
of lines and agglomeration of the stations on the lines is needed. Since the CNA is not the offi-
cial request, and it is too early especially for freight railway undertakings to specify the precise
location, the agglomerated stations should be provided in the master data (infrastructure topol-
ogy from BigData database).
  Minimal requirements
  CNA Excel file structure for the creation and import CNA data. According to this Excel file
structure, the ECMT functionality for the import and export must be developed.
  Additional requirements
  No additional requirements defined.
1.10.2 5.3.2. Integration options
Figure 15 – Capacity Needs Announcements data exchange
Minimal requirements
Object updates
  Capacity needs announcements can be edited until submission. The capacity needs an-
nouncements can be adapted after submission in order to provide the IM with more precise
data for planning of the capacity. The objects that should be exchanged are the following: Train,
Number of trains per line.
Schema and message definition
   The minimum data set in the message is the dataset given in the CNA Excel file structure de-
fined in Annex 8.
 CapacityModelMessage was defined to be used for providing data about the Capacity Model
Objects (CMOs), but also Capacity Needs Announcements (CNAs).
Endpoint and communication
  Capacbe usedb
       ➢ CNA -> Capacity Hub (input data) – sending of CNA objects via the CNA Excel file
structure.
1.10.3 5.3.3. Connection to other modules
  Minimal requirements
  Connection to Capacity Hub to exchange the CNA Excel file information.
## 1.11 5.4. Path Request Management (PRM)
  Preparation and harmonization of path request between the partners, based on the train pre-
pared in Path Request Management module, including the capacity products indication, will re-
sult in the construction and delivery of the path request in high quality.
   The RNE Path Coordination System (PCS) is developed towards preparation and harmoniza-
tion, as well as submitting of Path Request. The new release of PCS EC designed for "Envelope
Concept", does the sub-path division of the requested timetable of the train, without touching
the Train object. It solely focuses on the preparation and harmonization of the "clean" path re-
quest, which helps IMs to deliver the draft and final timetables more quickly and efficiently.
## 1.11.1 5.4.1. Centralized master data
  It is necessary to have access to the Master data for communication with IMs in order to be
able to indicate the operation points (infrastructure topology from BigData database) in the path
request.
  Minimal requirements
  Current PCS EC version.
  Additional requirements
  Requirements for the PCS Capacity Broker.
1.11.2 5.4.2. Integration options
Figure 16 - Path Request Management module data exchange
  The communication with Path Management module, currently integrated into the PCS sys-
tem, is necessary in the case of separation of the modules. The messaging must be established
within the TAF/TAP TSI framework for the path request process.
  Minimal requirements
Object updates
  Harmonization of path request objects between RU partners.
  Communication with the Path Management module by sending the path request and receiving
the path details information.
  Objects that will be used are a train, path, rolling planning slot, TCR, number of trains per line.
Schema and message definition
  The full range of TAF/TAP TSI framework messages like Path Request, Path Coordination, Path
Details, Path Confirmed, Path Details Refused, Path Cancelled, Object Info and Update
Link messages must be supported.
Endpoint and communication
  Path Management (PCS)
       ➢ PRM -> PM (input data) – communication via standard TAF/TAP TSI messaging frame-
work using PathRequestMessage, PathCoordinationMessage, PathConfirmedMes-
sage, PathDetailsRefusedMessage, ObjectInfoMessage, ReceiptConfirmation-
Message
       ➢ PM -> PRM (output data) – communication via standard TAF/TAP TSI messaging
framework using PathDetailsMessage, PathCoordinationMessage,
PathCanceledMessage, UpdateLinkMessage
▪
       ▪
  Additional requirements
  Communication to Capacity Broker - to be defined for inquiry of capacity
## 1.11.3 5.4.3. Connection to other modules
  Minimal requirements
  Connection or integration with Path Management module
  Additional requirements
  Connection to Capacity Broker module
       Path Management (PM)
  Path Management module has all the functionalities to work with the path requests and to
harmonize them. It further optimizes international path coordination by ensuring that path re-
quests and offers are harmonized by all involved parties. It will work together with Broker mod-
ule to harmonize paths for all RUs requests.
  The RNE PCS is an application for path requests, path management and pre-constructed
product publication. It is an international application for ordering and harmonizing new and late
path requests, ad-hoc path requests and feasibility study requests. PCS is also used for path
modification and alteration.
1.11.4 Centralized master data
  Initially, the PCS EC was developed with a stand-alone infrastructure dataset. The following
information was stored:
      •   Operation points with names, country ISO and Primary Location Codes
      •   The validity period of operation points
  Considering some international Rail Topo Models (e.g. RINF, IRS 30100), we can say that the
tool is prepared for macro level infrastructure data, but only with nodes. For the future, the im-
plementation of a mezzo level network model is necessary.
   Currently, the PCS EC synchronizes the primary location codes that are used for communica-
tion via TAF/TAP TSI.
  Minimal requirements
  As part of TTR implementation, PCS shall be connected to RNE's central database for topol-
ogy, called RNE BigData. Further on the locations and any other infrastructure related data
should be originated from RNE BigData.
  Code lists related to PathInformation element shall be synchronized from a Central Reposi-
tory, e.g. TrainActivity, BrakeTypes, NetworkSpecificParameters, etc.
  Additional requirements
  Based on the defined stops (operation points), PCS shall always generate a route. In the time-
table, only the stops shall be presented, rest shall be stored in the route (e.g. run through
points).
  PCS shall check if the defined PlannedTrainTechnicalData fits the selected geography (RNE
BigData requirement):
      •   Catenary information
      •   Gauge information
      •   Speed information
      •   Etc.
1.11.5 Integration options
Figure 17 - Path Management module data exchange
  Minimal requirements
Object updates
  Capacity band object with its process shall be implemented.
  Process steps and harmonization shall be moved to the path level (sub-path in PCS) from the
dossier Applicant - IM pair level.
  Case Reference (dossier in PCS) shall be able to contain paths with different process types
and harmonization status.
  Path object shall be updated to the latest state of PathInformation element of TAF/TAP TSI.
Schema and message definition
  XSD proposal for capacity handling (fields for pre-arranged paths, catalogue paths, capacity
bands).
  Message proposal for handling catalogue paths shall be defined for TAF/TAP TSI (not neces-
sarily the new message, but perhaps new Type Of Information (TOI), new optional fields).
  XSD proposal for a technical link among timetable period related objects for the multi-annual
request/allocation.
  Process proposal for Capacity updates shall be prepared (for catalogues and capacity
bands).
Endpoint and communication
  Path Request Management
➢ PRM -> PM (input data) – communication via standard TAF/TAP TSI messaging frame-
work using PathRequestMessage, PathCoordinationMessage, PathConfirmedMes-
sage, PathDetailsConfirmedMessage, PathDetailsRefusedMessage, ObjectIn-
foMessage, ReceiptConfirmationMessage, ErrorMessage
➢ PM -> PRM (output data) – communication via standard TAF/TAP TSI messaging
framework using PathDetailsMessage, PathCoordinationMessage,
PathCanceledMessage, UpdateLinkMessage, ErrorMessage, PathNotAvailableMes-
sage
  Capacity Hub
➢ CH -> PM (input data) – communication via standard TAF/TAP TSI messaging frame-
work using BandDetailsMessage, BandNotAvailableMessage, BandCoordination-
Message
➢ PM -> CH (output data) - communication via standard TAF/TAP TSI messaging frame-
work using BandCoordinationMessage (e.g. rolling planning)
  Capacity Broker
➢ CB -> PM (input data) - communication via standard TAF/TAP TSI messaging frame-
work using PathCoordinationMessage, PathDetailsRefusedMessage, PathNotAvaila-
bleMessage, PathDetailsConfirmed
➢ PM -> CB (output data) – communication via standard TAF/TAP TSI messaging frame-
work using PathCoordinationMessage
  Common Interface endpoint shall be opened for PCS.
  PCS shall be able to send and receive TAF/TAP TSI messages related to working with the avail-
able objects. Foreseen messages: ReceiptConfirmation, ErrorMessage, PathDetails, PathCoor-
dination, PathRequest, PathDetailsConfirmed, PathDetailsRefused, PathNotAvailable, Update-
Link, ObjectInfo, new messages for capacity bands.
## 1.11.6 Connection to other modules
  Minimal requirements
  Capacity Hub module
     •     Paths, catalogue paths and capacity bands shall be created and updated (also in case
of request) in Capacity Hub.
  Path Request Management module
     •     Path Requests shall be taken over from the Applicant part (PRM module) of PCS to the
Path Management of PCS
     •     Path Request and Path Details shall follow the same process
      •   Next to the Path ID, the Path Request ID shall be also stored in the path object of the
Path Management
      •   PCS (PM) shall allow linking several Path Details to one Path Request
      •   The link between the Train object and the path objects shall be stored and ensured
      •   After allocation PCS shall be able to generate daily objects, daily path variants and
provide the data to the Train harmonization.
      ▪
  Capacity Broker module
      •   PCS shall keep a dedicated part where the Broker can store the running timetable cal-
culations. This functionality could be useful because later on it will be possible to
compare to response from the Broker and official offer from the IM in the timetable.
      •   PCS shall allow the Broker to update its timetables with the received running time cal-
culations.
      ▪
      ▪
      ▪
      ▪
  Additional requirements
  TCR module
      •   Potential conflicts with TCRs shall be shown during the path request and path elabo-
ration phase
## 1.11.7 Enhancement of the functionality
  Minimal requirements
Multi-annual allocation
  A new technical link shall be implemented among timetable period related objects (dossiers,
sub-paths).
   Rolling Planning as a composite process type shall be implemented (composite = the process
itself can be shorter, longer depending on the date of submission and the first day of operation.
In some cases, it's a simple path request process, in other cases several rounds of iteration with
capacity request and allocation)
  The right part (version) of the composite Rolling Planning process shall be applied for paths
according to date of submission and the first day of operation.
Daily calendar
  Calendar days of a path, catalogues and capacity bands shall be stored in a daily calendar in-
stead of the currently used bitfield.
  For permanent support of the TAF/TAP TSI message, until they are not changed, the daily cal-
endar shall be exported to bitfield.
  Additional information shall be stored for the running days, like:
      •   Number of available slots for capacity bands
      •   Different flags of allocation for paths
  Additional requirements
Multi-annual allocation
  A new view shall be implemented where all objects, related to one of the new technical links,
can be handled.
## 1.11.8 Central implementation
  As a central tool, PCS already supports all necessary TAF/TAP TSI messages needed for path
requesting and harmonization.
  The multi-annual request, for multiple timetable periods, should be developed in the PCS.
Currently, in the PCS, it is possible to create a rolling planning request only for one year (yearly
timetable) using the “Rolling planning” process type. In this case, the same messages that are
used for the path request and harmonization shall be used for the rolling planning as well.
  Requesting a multi-annual path, users should be able to select “Rolling planning” process
type any time of the year for any timetable period (max 36 months) that is available for dossier
creation.
  The development of the multi-annual path should take into account the following:
## 1.11.9 National implementation
   This topic provides a guide for IMs/ABs to synchronize their timetables with PCS from the na-
tional systems. It does not cover the whole RU – IM communication, because of the focus on the
PCS – IM communication as a special dialect of the TAF-TSI communication.
  The following messages should be integrated:
 Message                            Description
 Path Request Message               PCS will deliver the path requests to the IMs with Path Request
messages
 Path Details Message               IMs can send all of their offers to PCS with Path Details mes-
sage
 Path Confirmed Message             PCS will deliver the information about the acceptance of the Fi-
nal Offer to IMs with this message
 Error Message                      In case there is any mistake regarding the update, PCS will send
back Error messages with PCS specific error codes inside
•   PCS will deliver the information about the rejection of the Final
 Path Details Refused Message
Offer to IMs with this message
•   IMs can send all of their updates before the offer with Path Co-
 Path Coordination Message
ordination message. Also, all notifications from PCS (except RU
originated) are sent via Path Coordination message
•   IMs need an option to delete an existing path in PCS. They can
 Path Not Available Message
do it with this message
  It is important to highlight that the Receipt Confirmation message is not mentioned in the list.
For PCS communication it’s not necessary. It’s important to state that PCS is a synchronous
system, meaning there is no option for waiting for a message for minutes, hours or sometimes
even days. For further details, please check the special use case that deals with the Receipt
Confirmation message.
## 1.11.9.1 Identifiers in PCS
  PCS is prepared to handle TAF TSI identifiers and even more, they are mandatory in the dossi-
ers. However, as most of the agencies are not ready yet to work with these identifiers, the sys-
tem is able to generate them automatically. The logic is simple; when the user wants, it’s possi-
ble to include the id, if not, the system takes care of it. Let’s see the logic in practice for the dif-
ferent identifiers.
TRAIN ID
   During the dossier creation and in the Open phase, the dossier creator agency is allowed to
select the company whose company code will be part of the Train ID. Then it’s also possible to
define the core element. The timetable period is coming automatically from the timetable pe-
riod of the dossier (also known as Case Reference). The variant is set to 00 by default.
  When the user creates the path requests in the dossier (called as sub-paths in the Applicant
timetable) and their origin/border/destination varies, then the user must link them to different
Train ID variants.
What happens, when the user doesn’t provide the Train ID?
  PCS will generate a Train ID. If the creator agency has UIC ID, then it will be added as com-
pany code, if not, then PCS will add 3178 as RailNetEurope. For the core element, the dossier ID
will be used, and the variants are automatically increased as a sequence if there is a change in
the origin/border/destination.
  <Example>
       TR - 3178 - ******215987 - 00 - 2020, where 215987 is the dossier ID.
  </Example>
PATH REQUEST ID / PA ID
  There is a slight difference compared to the Train ID. Companies can still define their own PR
ID/PA ID, but only if it’s a machine to machine communication, meaning via an interface. PCS
has its fields in the schema, called tsi_path_id. This field keeps the PR ID in the Applicant time-
table, and the PA ID in the IM timetable.
  What happens, when the user doesn’t provide the PR ID/PA ID?
  It can happen quite easily. Basically, it is the case for all the GUI users. PCS follows the same
logic as for the Train ID, but now not the dossier ID will be inserted to the core element, but the
PCS path ID, which is an internal PCS identifier. Company codes are entered with the same
logic as for the Train ID, but the variants here are always 00.
  <Example>
       PA - 3032 - ******488078 - 00 - 2020.
       We may check it more details in the Path Request paragraph.
 </Example>
   For IMs, it’s important to store all the IDs you receive, because you should send them back.
Also, please note that as you can see, the system is able to handle your own, national identi-
fiers.
## 1.11.9.2 Network-specific parameters vs National IM parameter
  PCS and TAF TSI have the common option and these are apart from the common train param-
eters each IM has the possibility to define his national IM parameters for covering the national
particularities. These parameters can be defined on the following levels:
   •   Dossier level: in TAF TSI this would mean message level network-specific parameter
o   Example in PCS XML schema:
…
<processtype_id>H</processtype_id>
<national_im_parameter id="78118">
<name>D01 - Vertriebskanal</name>
<value>PCS</value>
</national_im_parameter>
…
o   Example in TAF TSI XML schema:
…
</ns1:PathInformation>
<ns1:NetworkSpecificParameter>
<ns1:Name> D01 - Vertriebskanal </ns1:Name>
<ns1:Value> PCS </ns1:Value>
</ns1:NetworkSpecificParameter>
</ns1:PathCoordinationMessage>
   •   Path section level: in TAF TSI this would mean planned journey location level network-spe-
       cific parameter
Both are working with a name/value pair. In addition to the TAF TSI options, in PCS the
       IMs can define the required format of the parameter and the application checks their
       entry format. That is why it is needed always respect the defined format of the parame-
       ter, otherwise, PCS will send back an error message.
       Possible formats in PCS:
o   String
▪ Min numbers of characters >= 1
▪ Max number of characters <= 256
o   Single choice list
o   Multiple choice list
o   Number
▪ Min number of digits >= 1
▪ Max number of digits <= 12
o   Date
▪ dd.mm.yyyy
▪ yyyy-mm-dd
o   Time
▪ hh:mm:ss
▪ hh.mm
o   Datetime
▪ dd.mm.yyyy hh:mm:ss
▪ dd.mm.yyyy hh:mm
## 1.11.9.3 Loco Types
  Since version 2.2.3. loco types are handled with a composite identifier in TAF TSI. In PCS, it’s
also possible for the IMs to publish that are allowed to run on their network (it was developed a
couple of years before the TAF TSI XSD change). However, there is a slight difference between
the two structure. Let’s check first the TAF TSI structure.
•   Type Code 1: it’s always 9
•   Type Code 2: general vehicle type
o 0 Miscellaneous
o 1 Electric locomotive
o 2 Diesel locomotive
o 3 Electric multiple-unit set (high speed) [power car or trailer]
o 4 Electric multiple-unit set (except high speed) [power car or trailer]
o 5 Diesel multiple-unit set [power car or trailer]
o 6 Specialised trailer
o 7 Electric shunting engine
o 8 Diesel shunting engine
o 9 Special vehicle
•   Country code
•   Series number
▪
  PCS has its own TAF TSI generator and there the locos are stored in this format, but as they
are stored originally in PCS, not all the options are supported. The main difference is the Type
Code 2 because PCS stores that information in three fields and can generate the code based on
their combination:
•    Engine type: diesel, electric, hybrid, steam
•    Top speed
•    Multiple units: yes/no
  From this information, PCS can tell whether a loco is in Type Code 2 “1”, “2”, “3”, “4” or “5”,
but not more than that.
    Country code is originated from the IM’s UIC ID that published the loco type.
   The series number is not always kept in 4 digits because not every IM was able to publish their
loco types with UIC numbering. If you are planning to use TAF TSI communication, please check
first your loco types in PCS.
## 1.11.9.4 Example description
   The example mentioned in the scope topic (see 1.2. topic), shows a PCS dossier (216481)
that contains two trains (one core element with two variants). The example itself is a 2RU - 2IM
situation with ÖBB (0081) - RCA (2181), VPE (3032) - GYSEV-C (2143) pairs. The leading agencies
are in the second pair.
  The reason for the two trains is the two different destinations in Hungary (please check the
outline below). The trains are split according to weekdays (1-5) and weekends (6-7).
  RCA splits its path requests according to the trains. However, GYSEV-C has two path requests
for the second train variant, one for Saturdays (6) and one for Sundays (7).
  ÖBB prepares one-one paths as an offer to the received path requests. VPE sends two paths
for the weekdays; one for the summer period and for the rest. However, the requests for the
weekends cannot be met, that is why they send offers only for Saturdays.
  The example tries to demonstrate also the actions when a partner finished already the harmo-
nization (set green light in PCS), however, the neighbor changes something at the border. In this
case, PCS (the application itself) automatically downgrades the harmonization status of the
partner.
## 1.11.9.5 Objects
The following objects will be used during the example.
   For the demonstration purpose different kind of concepts are used for the identifier genera-
tion:
      •   RCA prepares the different path requests with the same core element and utilizes the vari-
ants to make differentiation
      •   GYSEV-C generates always fully random (but unique) core elements for the Path Requests
and leaves the variant 00.
      •   ÖBB, similar to RCA, prepares the different paths with the same core element and utilizes
the variants to make differentiation
      •   VPE, similar to GYSEV-C, generates always fully random (but unique) core elements for
the paths and leaves the variant 00.
## 1.11.9.6 Use case overview
  Before jumping into the details, please find here an overview of the use cases that we tried to
cover with this example.
  Path Request
  1. Get information about Path Request
  2. Pre-accepted Offer
  Path Elaboration and Post-Processing
  3. Timetable updates as work in progress
  4. IM would like to delete a path from PCS
  5. Setting green light in PCS and informing the partner IMs
  6. Sending an update affecting the border
  7. Offer (draft or final) is sent to the Applicants
  8. Setting red light in PCS and informing the partner IMs
  9. Rejecting dossier in PCS
  10. Sending an update that contains calendar days of existing paths
  11. Leading applicant withdraws the dossier
  12. Leading IM closes the dossier
  Observation and Acceptance
  13. IM gets back the dossier from Observation
  14. Leading applicant closes the dossier
  15. Applicant’s decision in the Acceptance phase
  Active Timetable
  16. Close dossier
  Receipt Confirmation and Error message
  17. The necessity of Receipt Confirmation
  18. Error Message from PCS
  19. Error Message to PCS
  Path modification / Path Alteration
  To be developed in a separate Handbook document.
## 1.11.9.7 Path Request
 Applicants have already finished their harmonization and they submitted path requests in
PCS. Let’s check what happens in the different process types:
     •   New Path Request: dossier arrives at Path Request phase
     •   Late Path Request: dossier arrives at Path Elaboration phase
     •   Ad-Hoc Path Request (currently supported via TAF-TSI connection): dossier arrives at Path
Elaboration phase
     •   Rolling Planning Path Request: dossier arrives at Path Elaboration phase
      •   It’s clear that for most of the process types the “Path Request” is only a milestone. The ex-
ception is the New Path Request where the leading IM has to release the dossier to Path
Elaboration (change request is pending to change this too). To do that the Leading IM can
send Path Coordination to PCS with TOI 07 (create offer). In this case, the message has
the following elements:
      •   Message header:
      •   Sender: 3032 (VPE as leading IM)
      •   Recipient: 3178 (RNE)
      •   TOR: 2 – Request
      •   MS: 1 – Creation
      •   TOI: 07 – Create an offer
      •   Identifiers: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00 - 2020; PR -
2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 – 2020
  It’s enough to have one message with one path. It doesn’t matter how many paths are exist-
ing, because the IM makes its actions on the dossier or on the dossier Applicant – IM pair.
What happens in PCS?
  It’s more important to note what happens in PCS when the leading Applicant sends the dos-
sier to path request. PCS copies the Applicant timetable to an IM timetable block.
 In our example there are five path requests (2+3), which are represented with sub-paths in
PCS:
      •   ÖBB receives PR - 2181 - ********ABCD - 00 - 2020; PR - 2181 - ********ABCD - 01 - 2020
      •   VPE receives PR - 2143 - *******X12C3 - 00 - 2020; PR - 2143 - *******98A01 - 00 - 2020;
PR - 2143 - *******SDF53 - 00 – 2020
      ▪
  As part of the copy of the timetables, PCS creates five sub-paths in the IM timetable. How-
ever, as the TAF TSI identifiers are mandatory and there was no IM activity so far in the dossier, it
must generate its own PA IDs:
      •   ÖBB: PA - 0081 - ******412000 - 00 - 2020; PA - 0081 - ******412001 - 00 - 2020
      •   VPE: PA - 3032 - ******412002 - 00 - 2020; PA - 3032 - ******412003 - 00 - 2020; PA - 3032 -
******412004 - 00 – 2020
  When an IM doesn’t have an interface connection, the GUI user can start working on these
paths immediately. However, if the IM has a connection, the first moment when they start send-
ing their own IDs, PCS deletes the prepared sub-paths. After that, when the IM sends with IDs
that are already known by PCS, PCS will update the paths accordingly, because the system
knows the ID pairs, e.g.:
PCS path ID 412005 = VPE path PA - 3032 – M20205405120 - 00 - 2020
Get information about Path Requests
  We already saw what happens in PCS, but how does the IM get information that path requests
arrived? In this case, PCS broadcasts Path Request messages to the affected IMs as the follow-
ing.
  To ÖBB two messages will be sent.
      •   Message header:
o Sender: 2181 (RCA)
o Recipient: 0081 (ÖBB)
o Broker: 3178 (RNE) with PCS LI number
      •   TOR: 2 – Request
      •   MS: 1 – Creation
      •   TOI: 4 – Harmonization completed
      •   Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2181 - ********ABCD - 00 - 2020
o Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2181 - ********ABCD - 01 – 2020
▪
  To VPE three messages will be sent.
      •   Message header:
o Sender: 2143 (GYSEVC)
o Recipient: 3032 (VPE)
o Broker: 3178 (RNE) with PCS LI number
      •   TOR: 2 – Request
      •   MS: 1 – Creation
      •   TOI: 4 – Harmonization completed
      •   Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******X12C3 - 00 - 2020
o Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2143 - *******98A01 - 00 - 2020
o Third message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******SDF53 - 00 – 2020
      ▪
  The content of the messages is trivial. They get Train Information and Path Information. It’s up
to them, whether they use the Train Information element, but the Path Information shall be
imported to their system. As said before, all the IDs shall be stored, because they must be used
later when the IM provides an answer. Just as a reminder, the IM cannot answer two path re-
quests with one path.
Pre-accepted offer
  PCS is able to handle Ad-Hoc path requests with a pre-accepted offer; however, it’s handled
with a special process type. Right at the beginning of the dossier creation, the leading Applicant
shall select the Ad-Hoc path request process with a pre-accepted offer.
 In this case, the IMs get this information with the Path Request messages. For example, ÖBB
would get two messages with the following information.
      •   Message header:
o Sender: 2181 (RCA)
o Recipient: 0081 (ÖBB)
o Broker: 3178 (RNE) with PCS LI number
      •   TOR: 2 – Request
      •   MS: 1 – Creation
      •   TOI: 19 – Pre-accepted offer
      •   Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2181 - ********ABCD - 00 – 2020
o Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2181 - ********ABCD - 01 – 2020
## 1.11.9.8 Path Elaboration and Post-Processing
  The below written use cases are valid basically for Path Elaboration and Post-Processing, but
most of them can be used in any phase where the IMs have editing rights, e.g. Feasibility Study
Elaboration, Path Modification Elaboration, Path Alteration Conference.
  Timetable updates as work in progress
  In this step, the IMs are updating their timetable in PCS. In this example, it will be demon-
strated with VPE. As presented in the Objects part, VPE has three paths to offer. To do so, VPE
shall send three Path Coordination message like the following. Please note that as we wrote
previously, even though VPE will send three paths, it doesn’t mean they send one for each path
request. They could not make the Sundays, but they send different offers for the weekdays.
  Three Path Coordination messages with:
      •   Message header:
o Sender: 3032 (VPE)
o Recipient: 3178 (RNE)
      •   TOR: 2 – Request
      •   MS: 1 – Creation
      •   TOI: 2 – Harmonization in process
      •   Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M20205405120- 00 - 2020
o Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M202054092*0- 00 -
2020
o Third message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 –
2020
▪
   This mechanism can be used either for creating new paths in PCS or updating an existing one.
With this action, the IM can create or update the timetable, parameters or calendar of the path.
If a location is not mentioned in the path that was part before, PCS will interpret as a delete. For
calendar changes, the IM shall send only a new calendar bitfield with the message.
  As part of the notification service, PCS will send (forward) these messages to ÖBB (or all in-
volved IM of the dossier, if there are more) as Path Coordination messages like the following.
      •   Message header:
o Sender: 3178 (RNE)
o Recipient: 0081 (ÖBB)
      •   TOR: 2 – Request
      •   MS: 1 – Creation
      •   TOI: 2 – Harmonization in process
      •   Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M20205405120- 00 - 2020
o Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M202054092*0- 00 -
2020
o Third message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 –
2020
  IM would like to delete path from PCS
   Normally, with PCS web-services this action is quite simple. The IM can use the updateDossi-
erRUIMPair operation and it should not mention the particular paths. PCS will interpret this ac-
tion as a delete. In TAF TSI, the messages are sent path by path, meaning there is a need for a
dedicated message to cancel a path in PCS.
  The IM can rely on the Path Not Available message with the following content.
      •   Message header:
o Sender: 3032 (VPE)
o Recipient: 3178 (RNE)
      •   TOR: 2- Request
      •   MS: 1 – Creation
      •   TOI: 21 – No alternative available
      •   Identifiers:
o CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00 - 2020; PR -
2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 – 2020
      ▪
  With this action VPE would delete PA - 3032 – M202062114312- 00 – 2020 from the dossier.
 As part of the notification service, PCS will send (forward) these messages to ÖBB as Path
Not Available message as the following.
      •   Message header:
      •   Sender: 3178 (RNE)
      •   Recipient: 0081 (ÖBB)
      •   TOR: 2- Request
      •   MS: 1 – Creation
      •   TOI: 21 – No alternative available
      •   Identifiers: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00 - 2020; PR -
2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 – 2020
Setting green light in PCS and informing the partner IM about it
   Changing the harmonization status is important in PCS and TAF TSI either. There is a slight dif-
ference that in PCS the harmonization status (the so-called acceptance indicators) are set on
the pair level and not on the path level. Meaning, it’s enough to send only one of the paths with
its identifier. It saves communication on the IM side but also needs an adaptation. Be careful
that until you are not done with your paths, don’t send this message to PCS, because it will set
your light to green for all included paths of yours.
 Setting the green light is one thing, but PCS will inform your partners about this action. That is
why they can expect message broadcasting from PCS on behalf of you. Let’s see an example,
when VPE would set a green light in the dossier and PCS would inform ÖBB about this action.
  VPE shall send one Path Coordination message.
      •   Message header:
o Sender: 3032 (VPE)
o Broker: 3178 (RNE)
      •   TOR: 2 - Request
      •   MS: 1 – Creation
      •   TOI: 04 – Harmonization completed
      •   Identifiers:
o CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00 - 2020; PR -
2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 – 2020
▪
  ÖBB will get three Path Coordination messages.
      •   Message header:
o Sender: 3178 (RNE)
o Broker: 0081 (ÖBB)
      •   TOR: 2 - Request
      •   MS: 1 – Creation
      •   TOI: 4 – Harmonization completed
      •   Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M20205405120- 00 - 2020
o Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M202054092*0- 00 -
2020
o Third message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 –
2020
Sending an update affecting the border
  How to send an update is covered in one of the previous use cases. We know how to do that;
however, it was not yet mentioned, what happens in PCS when an IM sends an update that
changes something on the border from the following element:
      •   Timetable
      •   Dwell time
      •   Calendar
      •   Location
      ▪
  PCS, the application itself, checks this action, and by design, it downgrades the green light of
the affected IM to yellow. It must be communicated via TAF TSI messages too. PCS will send all
the paths of the initiator IM to the affected IM. It can check then the impact, and if there is noth-
ing else to do, it can set a green light again as explained in another use case.
  Let’s take our example. ÖBB sends an update that changes something at Nickelsdorf grenze,
the border location. The sent message looks like as it’s described in the “Timetable updates as
work in progress” step.
  VPE receives two Path Coordination messages from PCS.
      •   Message header:
o Sender: 3178 (RNE)
o Recipient: 3032 (VPE)
      •   TOR: 2 - Request
      •   MS: 1 – Creation
      •   TOI: 4 – Coordination update
      •   Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2181 - *********ABCD - 00 - 2020; PA - 0081 – **M-AMA12345- 00 -
2020
o Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2181 - *********ABCD - 01 - 2020; PA - 0081 –**M-AMA12345- 01 –
2020
▪
  As you can see, the notification works pretty much the same as for the “Timetable updates as
work in progress” use case, however, it’s important that VPE is aware of the fact, its light was
downgraded to yellow.
Offer (draft or final) is sent to the applicants
  It’s a privilege of the leading IM to send out Draft Offer and Final Offer. However, as we are us-
ing PCS in this example, it’s also possible to rely on PCS’ automatic promotions. Regarding the
New Path Request process, when all the IM lights are green, it promotes the dossier to Draft Of-
fer and Final Offer on the deadline according to the RNE timetabling calendar. For Late Path Re-
quest and Ad-Hoc Path Request process it checks also the timetabling calendar first (for LPR we
have to be after the NPR deadlines) and it promotes the dossiers that are ready, every midnight.
  The Applicants will receive the Path Details messages with the proper information. If that’s
the case, PCS shall only send the notification to the IMs that Draft Offer or Final Offer was sent
out.
  Please note that Draft Offer is used only in New Path Request and Rolling Planning processes.
For the others, the Final Offer is used only and those are followed by the Acceptance phase.
  IMs will get back Path Coordination messages about their paths.
      •   Message header:
      •   Sender: 3178 (RNE)
      •   Recipient: 3032 (VPE)
      •   TOR: 2 – Request
      •   MS: 1 – Creation
      •   TOI: 09 – draft offer or 16 – final offer
      ▪
  If the leading IM insists to do this action alone, not relying on PCS, it can send also one Path
Details message (we talked already about the difference between dossier and paths) with the
proper type of information.
  In our example, VPE should send the following Path Details message.
      •   Message header:
o Sender: 3032 (VPE)
o Recipient: 2143 (GYSEV-C)
o Broker: 3178 (RNE) with PCS LI number
      •   TOR: 2 – Request
      •   MS: 1 – Creation
      •   TOI: 09 – draft offer or 16 – final offer
      •   Identifier:
o CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00 - 2020; PR -
2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 – 2020
  As it’s a change, affecting the whole the dossier, the same notification will be sent out by PCS
to the participating IMs as above.
Setting red light in PCS and informing the partner IM about it
  From the PCS point of view, there is not such a big difference between setting green or setting
red light in the system. The procedure is quite similar to in the other use case. It’s enough to
send one Path Coordination message with the proper information. There is only one additional
thing: PCS requires a mandatory comment for the reason of the rejection. This must be pasted
to the Path Coordination message, into its free text field on the message level.
  If VPE likes to set a red light, the following Path Coordination message should be sent.
      •   Message header:
o Sender: 3032 (VPE)
o Recipient: 3178 (RNE)
      •   TOR: 2 - Request
      •   MS: 1 – Creation
      •   TOI: 03 – Harmonization rejected
      •   Identifiers:
o CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00 - 2020; PR -
2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 – 2020
▪
  Again, same as for the green light, ÖBB will be informed about this change by PCS.
  ÖBB will get three Path Coordination messages, with the same free text field delivering the
reason for rejection.
      •   Message header:
o Sender: 3178 (RNE)
o Recipient: 0081 (ÖBB)
      •   TOR: 2 - Request
      •   MS: 1 – Creation
      •   TOI: 03 – Harmonization rejected
      •   Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M20205405120- 00 - 2020
o Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M202054092*0- 00 –
2020
o Third message: TR - 2143 - *********927 - 00 - 2020; PR - 2143 - *******98A01 - 00
- 2020; PA - 3032 – M202062114312- 00 – 2020
Rejecting dossier in PCS
  Leading IM has the chance to reject a dossier. The procedure is almost the same for all pro-
cess types, only the TOI is different. In the “New Path Request” process and “Rolling Planning”
Process we have “Draft Offer”, but in the others, we only have “Final Offer”. This is the main dif-
ference.
  In the New Path Request and Rolling Planning processes, the leading IM (VPE in our example)
shall send one Path Coordination message like the following.
      •   Message header:
o Sender: 3032 (VPE)
                   o Recipient: 3178 (RNE)
•    TOR: 2 - Request
•    MS: 1 – Creation
•    TOI: 43 – Preparation of draft offer rejected
•    Identifiers:
o CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00 - 2020; PR -
2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 – 20204
▪
  In Late Path Request and Ad-Hoc Path Request process, the leading IM (VPE in our example)
shall send one Path Coordination message like the following.
•    Message header:
o Sender: 3032 (VPE)
o Recipient: 3178 (RNE)
•    TOR: 2 - Request
•    MS: 1 – Creation
•    TOI: 15 – Preparation of final offer rejected
•    Identifiers:
o CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00 - 2020; PR -
2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 – 20204
▪
 As part of the PCS notification service, the system shall broadcast this information. Thus,
ÖBB will get two Path Coordination messages for its two paths (because the whole dossier
was rejected by the leading IM).
•    Message header:
o Sender: 3178 (RNE)
o Recipient: 0081 (ÖBB)
•    TOR: 2 – Request
•    TOI: 43 – Preparation of draft offer rejected/15 – Preparation of final offer rejected (de-
pending on the process type)
•    Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2181 - *********ABCD - 00 - 2020; PA - 0081 – **M-AMA12345- 00 –
20204
o Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2181 - *********ABCD - 01 - 2020; PA - 0081 –**M-AMA12345- 01 –
20204
4
    PA can be sent only if it's known. As it’s a rejection, it could be that it’s rejected immediately, before the IMs sent any
    of their own PA(s) to PCS.
Sending update that contains calendar days of existing paths
  It was already described how to send updates to existing paths. However, there is a special
use case, when an IM sends an update that affects the already existing paths in the system that
are not in the message. What are we talking about?
  Imagine a situation where you have two paths in PCS:
      •   First runs on 1-5
      •   Second runs on 6-7
      ▪
   Then the IM sends Path Coordination message with a new path for running days 5-6. PCS, as
the application itself, is avoiding double booking on the running days. Calendar days are working
as a switch by design. Even if the update arrives via an interface, this switch works, and PCS re-
moves the selected days from the other calendars. As previously mentioned, the change in the
dossier will result in Path Coordination messages to the participating IMs about the change.
Why is it then so special? Because, in this special case, the notification service is sent to the ini-
tiator IM too. He will also get the message(s) about the changed path(s). In this special case, he
will get two Path Coordination messages with their identifiers and the changed calendars.
Leading applicant withdraws the dossier
   Until the end of Path Elaboration, the leading Applicant has the chance to withdraw the dos-
sier. As always, it has to put there a mandatory reason to explain this action (similar to the rejec-
tion).
  In our example, GYSEV-C is the leading Applicant. If they decide to withdraw the dossier,
every participating IM shall get this information. PCS will send the information to the IMs on be-
half of their partner Applicants (sender). Let’s check this from VPE point of view. They will get
three Path Request messages.
      •   Message header:
o Sender: 2143 (GYSEV-C)
o Recipient: 3032 (VPE)
o Broker: 3178 (RNE)
      •   TOR: 2 - Request
      •   MS: 2 – Delete
      •   TOI: 29 - withdrawal
      •   Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M20205405120- 00 - 2020
              o   Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M202054092*0- 00 –
2020
o   Third message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 –
2020
▪
  The same is happening between RCA and ÖBB, regardless that GYSEV-C is leading because
the partner of ÖBB is RCA.
Leading IM closes the dossier
  If it becomes clear, for any reason, that there is no further need for a dossier, the leading IM
has the chance to close it. This option is available for them in Path Elaboration, Post-Processing
and also later in Active Timetable.
 In our case, VPE is the leading IM. It’s enough for them to send only one Path Coordination
message like the following.
      •   Message header:
o Sender: 3032 (VPE)
o Recipient: 3178 (RNE)
      •   TOR: 2 - Request
      •   MS: 1 – Creation
      •   TOI: 31 – Close dossier
      •   Identifiers:
o CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00 - 2020; PR -
2143 - *******X12C3 - 00 - 2020; PA - 3032 – M20205405120- 00 – 2020
▪
 As part of the PCS notification service, the system shall broadcast this information. Thus,
ÖBB will get two Path Coordination messages for its two paths.
      •   Message header:
o Sender: 3178 (RNE)
o Recipient: 0081 (ÖBB)
      •   TOR: 2 – Request
      •   TOI: 31 – Close dossier
      •   Identifiers:
              o   First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2181 - *********ABCD - 00 - 2020; PA - 0081 – **M-AMA12345- 00 -
2020
o   Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2181 - *********ABCD - 01 - 2020; PA - 0081 –**M-AMA12345- 01 -
2020
## 1.11.9.9 Observation and Acceptance
IM gets back the dossier from Observation
  Observations phase is available only in “New Path Request” and “Rolling Planning” pro-
cesses. In this phase the Applicants have max. 4 weeks to analyse the Draft Offer and provide
feedback. Please note that in PCS they can only make comments in a standardized way to the
paths they are concerned about.
 Then, the dossier is moved to the Post-Processing phase either by the leading Applicant or by
PCS’ automatic promotion.
  If you check it carefully, it means multiple notifications:
      •   When the Applicant makes a comment to a path (optional for them, not sure you get a no-
tification for this)
      •   When the dossier reaches Post-Processing, meaning IMs can edit it again
      ▪
  Let’s see what kind of messages would be sent by PCS in these situations.
  GYSEV-C made an Observation on a path and VPE gets a Path Request message.
      •   Message header:
o Sender: 2143 (GYSEV-C)
o Recipient: 3032 (VPE)
o Broker: 3178 (RNE)
      •   TOR: 2 – Request
      •   MS: 1 – Creation
      •   TOI: 12 – observation - complete
      •   Identifiers:
o CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00 - 2020; PR -
2143 - *******X12C3 - 00 - 2020; PA - 3032 – M20205405120- 00 – 2020
▪
  The Observation itself is delivered in the free text field of the Path Request message on the
message level. It happens only when a path is commented by an Applicant and this message is
sent only to the affected IM.
  When the dossier arrives at Post-Processing, each participating IM shall get this information.
That is why PCS will send out Path Coordination message to all IMs with their paths. Let’s
check it from VPE’s point of view. They will get three Path Coordination messages.
      •   Message header:
o Sender: 3178 (RNE)
o Recipient: 3032 (VPE)
      •   TOR: 2 - Request
      •   MS: 1 – Creation
      •   TOI: 07 – Create offer
      •   Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M20205405120- 00 - 2020
o Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M202054092*0- 00 –
2020
o Third message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 –
2020
  Leading applicant closes the dossier
  PCS provides the possibility for the leading Applicant in Observations phase to close the dos-
sier. In this case, the dossier will not go back to the IMs in the Post-Processing phase.
  In our example, GYSEV-C is the leading Applicant. If they decide to close to the dossier, every
participating IM shall get this information. PCS will send the information to the IMs on behalf of
their partner Applicants (sender). Let’s check this from VPE point of view. They will get
three Path Coordination messages.
      •   Message header:
o Sender: 317RNE
o Recipient: 3032 (VPE)
      •   TOR: 2 - Request
      •   MS: 1 – Creation
      •   TOI: 31 – Close dossier
      •   Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M20205405120- 00 - 2020
o Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M202054092*0- 00 –
2020
             o   Third message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 –
2020
  Applicants’ decision in Acceptance phase
  In Late Path Request and Ad-Hoc path request processes the applicants can make several
decisions after the first offer from the IMs:
     •   Accept the offer: in PCS, this would move the dossier to Active Timetable
     •   Reject the dossier: in PCS, this would move the dossier to Closed
     •   Send back to Path Elaboration
     ▪
  IM will get Path Confirmed messages if the Applicants accepted the Final Offer.
     •   Message header:
o Sender: 2143 (GYSEV-C)
o Recipient: 3032 (VPE)
o Broker: 3178 (RNE)
     •   MS: 1 – Creation
     •   TOI: 17 – final offer accepted
     •   Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M20205405120- 00 - 2020
o Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M202054092*0- 00 –
2020
o Third message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 –
2020
▪
  Please note that compared to the other messages, in Path Confirmed message, the following
elements are not required:
     •   Type of request
     •   Path Information
     ▪
     ▪
  IM will get Path Details Refused messages if the Applicants rejected the Final Offer.
     •   Message header:
o Sender: 2143 (GYSEV-C)
o Recipient: 3032 (VPE)
o Broker: 3178 (RNE)
     •   MS: 1 – Creation
     •   TOI: 25 – offer/final offer rejected (without revision)
     •   Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M20205405120- 00 - 2020
o Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M202054092*0- 00 –
2020
o Third message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 –
2020
▪
  Please note that compared to the other messages, in Path Confirmed message, the following
elements are not required:
     •   Type of request
     •   Path Information
     ▪
  IM will get Path Details Refused messages with special TOI if the Applicants return the dos-
sier to Path Elaboration.
     •   Message header:
o Sender: 2143 (GYSEV-C)
o Recipient: 3032 (VPE)
o Broker: 3178 (RNE)
     •   MS: 1 – Creation
     •   TOI: 27 – offer/final offer rejected (revision required)
     •   Identifiers:
o First message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M20205405120- 00 - 2020
o Second message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 -
00 - 2020; PR - 2143 - *******X12C3 - 00 - 2020; PA - 3032 – M202054092*0- 00 –
2020
o Third message: CR - 3178 - ******216481 - 00 - 2020; TR - 2143 - *********927 - 00
- 2020; PR - 2143 - *******98A01 - 00 - 2020; PA - 3032 – M202062114312- 00 –
2020
▪
  Please note that compared to the other messages, in Path Details Refused message, the fol-
lowing elements are not required:
     •   Type of request
     •   Path Information
1.11.9.10       Active Timetable
Close dossier
  In Active timetable, either the leading Applicant or the leading IM has the chance to close the
dossier. The procedure works exactly the same as it is written in the former close dossier use
cases.
## 1.11.9.11       Receipt Confirmation and Error message
The necessity of Receipt Confirmation
 As said before, PCS works as a synchronous system, that is why the Receipt Confirmation
message is not required.
  By default, when a dossier arrives first at an IM phase, the IM’s acceptance indicators are
blue. This is changed to yellow whenever there is an update from the IM.
  We do not support receiving Receipt Confirmation, thus please don’t send them when you are
communicating with PCS.
  Normally, the IM should confirm the Path Request with RC before the dossier is promoted to
the Path Request phase in PCS. This is done automatically in PCS (synchronous) and PCS sends
RC as a broker to the Applicant in the name of the IM to the Applicant.
Error message from PCS
  The integration of the Error Message is rather important. PCS has its own errors (link) and it
would send back to the IMs in the standard Error Message. For example, VPE would try to send
an update with an unknown traction mode, PCS would send back the following message.
     •      Message header:
o Sender: 3178 (RNE)
o Recipient: 3032 (VPE)
     •      MS: 1 – Creation
     •      TypeOfError: 1 – Functional (except 501, when it’s PCS system error, then it’s 2 – Tech-
nical)
     •      Severity: 2 – Fatal
     •      ErrorCode: 281 – Unknown traction mode
     •      FreeTextField: Error code description, e.g. The request contains traction mode which
can’t be resolved in the system.
Error message to PCS
 Same as for the Receipt Confirmation, we don’t support this.
## 1.11.9.12        Path Modification / Path Alteration
 To be developed in a separate Handbook document.
1.12 Temporary Capacity Restriction (TCR)
   To keep the infrastructure and its equipment in good condition (maintenance) and to allow in-
frastructure development in accordance with market needs, TCRs are necessary. Regarding the
Annex VII and TCR Guidelines, IMs are obliged to publish all known TCRs (national and interna-
tional) with major, high, medium or minor impact on the traffic.
  TCR module is a module for the international harmonization of all known TCRs. TCR module
has a possibility to visualize the TCRs and helps IMs in a coordination process to decrease the
negative influence of TCRs on the operation. The TCR is described by the location on the net-
work, the reason for the restriction, the time expansion, the operational consequences: traffic
impact, traffic measurements and classifications. The next important information transmitted
from this module is the coordinated and harmonized TCRs. Also, it gives the possibility to RUs to
comment TCRs in a consultation phase before the TCRs publishing. Since the Annex VII, all
TCRs should be published internationally and nationally.
  The RNE TCR Tool has developed with the goal of creating a single place with all information
about available TCRs. This tool will be used for a stepwise implementation, but also for the final
TTR IT Landscape implementation.
   The TCR tool will be fed by data from IMs' national systems via TAF/TAP TSI based technical
interface. In the case that IMs do not have a tool for the TCR management in their company,
they will be able to use already defined Excel file to import needed data. Also, there is a possibil-
ity to manually enter data via the Graphical User Interface (GUI) of the TCR tool itself.
Centralized master data
  RNE TCR tool is the first application connected to the centralized master data (the BigData)
database. Data that is used and synchronized from the BigData database is as follows:
      •   Primary Location Codes (PLCs)
      •   Segments
      •   Sections
      •   Section-Segment relation
      •   Companies
      •   Layers
  Minimal requirements
  For the import of TCRs, already developed Excel and XML file structure will be used. Compa-
nies that developed their own TCR systems, will be able to use the TAF/TAP based interface.
   TCRs must be manually coordinated between IMs and commented by RUs using the TCR Tool
and using the already implemented process and process steps. Information on the required ac-
tivity in the tool (IMs coordination or consultation in the case of RUs) and needs to do some ac-
tivity in the tool, will be sent to the user's e-mail.
Deviation route
  In the case of line unavailability because the TCRs, traffic could be re-rerouted. For this pur-
pose, it is possible to enter a deviation location and deviation border in the TCR tool. IM defines
the deviation location and deviation border within the own network, where the rail traffic shall
be re-routed.
  Additional requirements
 Some additional functionality, that will be defined by TCR WG members, could be imple-
mented or used in the tool, like the following:
       •   information about the number of tracks on the line,
       •   catenary information,
       •   inform all parties about deviation routes to optimize the deviations.
Integration options
Figure 18 – TCR module data exchange
  Minimal requirements
Object updates
  TCR object shall be extended with the mandatory content of the TCR object in the TAF/TAP TSI
schema.
  The structure of the import files (Excel, XML) shall be extended with additional TCR attributes.
  Additional needed messages for TCR handling (e.g. coordination process) could be defined
Schema and message definition
  XSD proposal of TCR shall be updated and defined for TAF/TAP TSI
  The TAF/TAP TSI messages’ proposal for handling TCR will be developed in the TCR Tool
Endpoint and communication
  BigData
       ➢ BD -> TCR (data synchronization) – topology data using the views exposed through
database synonyms (company, location, segment, section, layer)
  Capacity Hub
       ➢ TCR -> CH (output data) – the published TCRs data will be pushed to Capacity Hub
       ➢ CH -> TCR – communication via standard TAF/TAP TSI messaging framework using
SearchTCRMessage to receive information about specific TCRs or planned and coor-
dinated but not yet published TCRs
       ▪
  Capacity Broker
       ➢ TCR -> CB (output data) – the published minor TCRs data will be pushed to Capacity
Hub
       ➢ CB -> TCR – communication via standard TAF/TAP TSI messaging framework using
SearchTCRMessage to receive information about planned minor TCRs that are not
yet published. This message shall be used to get information about the maintenance
window.
       ▪
  Common interface endpoint shall be opened for TCR.
  TCR Tool shall be able to receive and send TAF/TAP TSI messages related to working with the
available objects. Foreseen messages: ObjectInfoMessage, TCRMessage, SearchTCRMessage,
TCRImportMessage
                   Connection to other modules
  Minimal requirements
  BigData module
      •   The topological data to select or present TCRs on a map at a line or location
      •   Data is synchronized using the views exposed through database synonyms (company,
layer, section, segment, location), on a monthly basis but, in case of need, it could be
more frequently (weekly or daily basis).
      •   The locations, segments and/or sections of the GeoEditor tool will be extended with a
custom attribute “TCR_IM_Coordination” which value will be used for the automatic
calculation of the involved IMs in the TCR coordination
  Capacity Hub module
      •   After the TCR coordination and publication, TCR data will be available to Capacity Hub
module internally (in the TTR IT central framework – central system) via the TAF/TAP
interface. The message that will be used is TCRMessage.
  Capacity Broker module
      •   Provide information about the planned minor TCRs
  After the implementation of the RINF data into BigData database, additional attributes shall
be incorporated into the TCR tool messages (like a number of tracks on the line, catenary infor-
mation and similar).
  Additional requirements
  In the future, all TCR data will be exchanged between other modules using only the TAF/TAP
TSI. For this purpose, the SearchTCRMessage and TCRMessage will be used.
Central implementation
  Currently, the TCR messages and objects do not exist in the TAF/TAP TSI schema. Due to a
large number of TCRs that should be imported into the TCR tool from the national systems, it is
not feasible to use the functionality of the manual creation or data import. Therefore, the
technical interface (TAF/TAP TSI based) between the national and central system must be im-
plemented.
  The TCR tool should support all the messages necessary to exchange the TCRs data.
National implementation
   This topic provides a guide for IMs/ABs to synchronize their TCRs with TCR tool from the na-
tional systems. The focus is on the IM – TCR Tool communication as a special dialect of TAF-TSI
communication.
  The following messages should be integrated:
 Message                               Description
 TCR Message                           IMs will deliver TCRs with their description to the RNE cen-
tral TCR tool
 TCR Canceled Message                  IMs can be able to cancel (deactivate) the TCR. TCR will not
be deleted but deactivated, and not able to be modified any
more.
 TCR Response Message                  This message is the response to a TCRMessage. It contains
the status and a report of the import (returns a syntax, se-
mantics and data validation information)
 Capacity Product Search Message       The IMs and RAs will be able to search for TCRs, but also
other capacity products, by specific parameters. They will
not ask for the information on the object (e.g. ObjectIn-
foMessage) but search all objects that meet the search cri-
teria.
•   This message is the response to a SearchCapacityProduct
 Capacity Product Message
message. It may contain information about the TCRs, Ca-
pacity bands, Catalogue paths or Pre-arranged paths,
matching the SearchCriteria defined in the SearchCapaci-
tyProduct message.
## 1.12.1.1 An identifier in the TCR Tool
  TCR Tool and messages are prepared to support TAF TSI identifier (TCRID element). During the
TCRs import, IMs are able to define the identifier’s parts by entering company code and TCR ID
as a core element. In the case of manually creating a TCR, only the TCR ID is required as the
core element and the company code will be added automatically by the tool, depends on the
login information.
The technical specification for the national implementation is provided in the Annex 4 of this
document.
## 1.13 Capacity Hub (CH)
  Capacity Hub module is a module for capacity modelling, planning and product development.
Capacity Hub module collects all data from the IMs (available capacities, bands and TCRs) and
RUs (capacity needs announcements) side and gives an overview on the available capacity and
TCRs in an early stage of planning. It safeguards capacity for RP requests, and it is responsible
for answering on the capacity requests in the advanced planning phase.
  Capacity Hub module has implemented some sort of AI which allows automatic detection of
affected companies and affected neighbours to give them information when something is
changed during the capacity planning. That is an intelligent help for the coordination. Automatic
conflict detection of the planned capacity and TCRs or RUs announcements and planned ca-
pacity when data is posting and give automatic information about that.
  RNE is working on TTR pilot’s application for Capacity Planning, called Electronic Capacity
Modell Tool (ECMT). As part of the stepwise implementation of TTR IT Landscape, the existing
applications could be taken into account.
  The below written high level summary of requirements that could bring ECMT to that level that
can fulfil the requirements of the TTR process and TTR IT Landscape.
## 1.13.1 Centralized master data
  Due to some constraints, the ECMT is being developed with a stand-alone infrastructure da-
taset. The following information is stored:
      •   Operation points with names, country ISO and Primary Location Codes
      •   Operation points linked to lines
      •   Operation points linked to each other with distances
      ▪
  Considering some international RailTopo Models (e.g. RINF, IRS 30100), we can say that the
tool is prepared for macro level infrastructure data, but in the future, at least meso level looks
reasonable.
  Minimal requirements
  As part of TTR implementation, ECMT shall be connected to RNE's central database for topol-
ogy, called RNE BigData. Further on the locations, lines and any other infrastructure related
data should be originated from RNE BigData.
  Line information in ECMT shall be extended with track number information (RNE BigData re-
quirement).
  New Capacity product messages should be implemented into the TAF/TAP TSI and necessary
interfaces developed.
  Provide the capacity model overview.
 Accept frequent ongoing update of slots for rolling planning (frequency to be defined –
minutes, hours, days…).
  Notification on border-times / parameters inconsistency, notification of the applicants on the
request inconsistency or capacity reduction.
  Additional requirements
  Route finder option shall be implemented in the tool and the chart or any other figures shall be
shown on the generated route.
  Route definition shall be also supported via point selection on map.
  Operation point information in ECMT shall be extended with geo location information.
1.13.2 Integration options
Figure 19 - Capacity Hub data exchange
  During the TTR pilots, ECMT is working with simplified objects. The following objects are avail-
able: path, catalogue path, TCR, capacity band. In the future ECMT shall be to communicate
with other modules, thus the simplified objects might be extended.
  Minimal requirements
Object updates
  Catalogue path and path object shall be extended with the mandatory content of the Path-
Information element of TAF/TAP TSI.
  TCR object shall be extended with the mandatory content of the TCR object of TAF/TAP TSI.
  Additional elements shall be added to the capacity band object, based on the catalogue path
and path object updates.
 Additional updates shall be done to the particular objects based on the structure of Capacity
Needs Announcements (see it later)
Schema and message definition
 XSD proposal of the capacity band shall be defined for TAF/TAP TSI
 Message proposal for handling capacity bands shall be defined for TAF/TAP TSI.
Endpoint and communication
 BigData
       ➢ BD -> CH (data synchronization) – topology data using the views exposed through
database synonyms (company, location, segment)
       ▪
 Capacity Needs Announcements
       ➢ CNA -> CH (input data) – information on the CNA objects – “Capacity wishes”. It’s
needed to investigate the CNA structure and based on the outcome update of exist-
ing objects or creation of new objects shall be done
 TCR
       ➢ TCR -> CH (input data) – information on the published TCRs will be received.
       ➢ CH -> TCR - communication via standard TAF/TAP TSI messaging framework using
SearchTCRMessage to query information about specific TCRs or planned and coor-
dinated but not yet published TCRs. The result will be received by TCRMessage.
       ▪
 Path Request Management
       ➢ PRM -> CH (input data) – Looking (search criteria) for published capacity facilities
       ➢ CH -> PRM (output data) – Information on published catalogue paths, capacity
bands and TCRs via standard TAF/TAP TSI messaging framework using CapacityPro-
ductDetailsMesssage
       ▪
 Path Management
       ➢ PM -> CH (input data) – communication via standard TAF/TAP TSI messaging frame-
work using the CapacityProductCoordinationMessage to exchange data about the
requests.
       ➢ CH -> PM (output data) - communication via standard TAF/TAP TSI messaging frame-
work to answer on the request using the CapacityProductCoordination, Capaci-
tyProductConfirmed, CapacityProductDetails, CapacityProductRefused, Capaci-
tyProductNotAvailable.
 Common Interface endpoint shall be opened for ECMT
  ECMT shall be ready to send and receive TAF/TAP TSI messages related to working with the
available objects. Foreseen messages: ReceiptConfirmation, ErrorMessage, PathRequest,
PathDetails, PathCoordination, TCRMessage, SearchTCRMessage, new messages for capacity
bands (CapacityProductCoordination, CapacityProductDetails, CapacityProductConfirmed,
CapacityProductDetailsRefused, CapacityProductNotAvailable) and the capacity needs an-
nouncements.
  Additional requirements
  Implementation of some sort of Artificial intelligence (AI) which allows automatic detection of
affected companies and affected neighbours to give them information when something is
changed during the capacity planning.
  Accepting and answering on RUs capacity needs announcements (recognize requests and al-
locate the safeguarded capacity if applicable).
  Update of the existing objects or creation of new objects with a view to a defined CNA struc-
ture
## 1.13.3 Connection to other modules
  ECMT itself is basically an application for presentation and publication of information. To
have that information, the tool must receive the used objects from other modules of TTR IT
Landscape. Please note that Capacity Broker will also work under the hood of the Capacity Hub,
but that functionality will be described in the Capacity Broker module.
  RUs and IMs have dedicated applications for certain objects and a communication stream
shall be established among those applications and ECMT.
  Minimal requirements
  BigData module
      •   The topological data
      •   Data is synchronized using the views exposed through database synonyms (company,
segment, location), on a monthly basis but, in case of need, it could be more fre-
quently
  Temporary Capacity Restriction module (TCR module)
     •    The TCR objects shall be received and updated from the TCR module
  Path Request Management module
     •    Published catalogue paths, capacity bands and TCRs shall be available for Appli-
cants. Capacity Hub shall be able to send this information to Path Request Manage-
ment according to its search criteria.
     •    As the host of the broker algorithm, the tool shall be able to receive feasibility study
requests. Further details are written in the Capacity Broker part.
  Train Harmonization module
     •    Published catalogue paths, capacity bands and TCRs shall be available for Appli-
cants. Capacity Hub shall be able to send this information to Train Harmonization ac-
cording to its search criteria.
     •    As the host of the broker algorithm, the tool shall be able to receive feasibility study
requests. Further details are written in the Capacity Broker part.
  Path Management module
     •    Paths, catalogue paths and capacity bands shall be received and updated from/via
Path Management.
  Additional requirements
  Capacity Needs Announcements module
     •    Investigation of the CNA structure
     •    Based on the outcome of the investigation update of existing objects or creation of
new objects shall be done, if necessary
     •    New views shall be prepared for CNA presentation.
      ▪
      ▪
## 1.13.4 Enhancement of the functionality
  Initially, ECMT is prepared as a publication tool for capacity products. However, with the im-
plementation of TTR IT Landscape, some additional features shall be added to help the capacity
planning (capacity calculation and optimization). UIC described a compression method in one
of their leaflets that shall be applied for this.
  Minimal requirements
  According to UIC Leaflet 406, ECMT shall be able to calculate
      •    Capacity on the node(s)
      •    Capacity on single track line section(s)
      •    Capacity on multiple track line section(s)
  ECMT shall present the calculation results in a table according to the selected geography
  Additional requirements
 ECMT shall show a classification of capacity consumption values on a selected network's
map.
  ECMT shall indicate conflicts among the objects, using the same capacity.
Capacity Broker (CB)
## 1.13.5 Introduction
   Capacity Broker module is a module for capacity inquiry and request. Capacity Broker module
uses harmonized Capacity Product Publication data as an input, and all inquiries and requests
from the RUs side will be validated due to it. Capacity Broker summarizes all requests from the
RUs side and gives the feedback if this requirement fits the available capacity or not, because
there could be a problem due to TCR. It will solve the RUs problem with creation and harmoniza-
tion of path requests due to maintenance works.
  Also, if the capacity is already booked, the Capacity Broker must be able to get this infor-
mation from the IMs national systems in real-time. Capacity Broker module will check the avail-
able capacity with national IT systems before the offer of the path through the Path Management
module. The final answer to the path requests should be done by IM and delivered back to the
Broker which will broadcast the message to RUs via Path Management module.
  Capacity broker doesn't exist as an application.
  As there is no existing solution, in the last paragraph called just "Functionality", because en-
hancement is not applied here, the list of functionalities will be provided.
1.13.6 Www.rail-booking.eu concept
  Using and working with the Central TTR IT Framework must be easy and understandable with
all the necessary information in one place. The approach should be similar to the booking sys-
tems to reserve a flight or a hotel room.
  The aim is to implement such a concept to the TTR IT Landscape, to support the European
booking system of the rail sector.
  As it was explained in the topic above (topic 3.8.1.), the Capacity Broker is the main module
that will deal with all Applicants requests and that shall be able to provide the first information
of the availability of their requests.
   The Applicants’ demand is to provide their requests to one tool only and receive the answers
from the same tool back. The manual data enter, for the Applicants who doesn’t have their na-
tional tools, should be possible as well. For this purpose, the functionality of the Booking
Frontend portal shall be implemented.
  The Capacity Broker will consult all the request created manually through the Booking
Frontend portal or sent by TAF/TAP TSI messages, check the pre-planned capacity in the central
system and consult national IMs systems, collect data (offers from involved IMs) and prepare
the harmonized offer to be presented on the portal and finally send to the Applicants systems.
Figure 20 - www.rail-booking.eu concept
  As all capacity requests of Applicants will be sent and collected in one central booking sys-
tem, also all offers provided by various IMs will be collected and composed in the same central
booking system.
  In general, two main functionalities of the rail booking concept are foreseen:
      •   Booking frontend portal
      •   Consulting the national systems
  Booking Frontend portal
  The portal will support the following functionalities:
      •   to show all created requests by Applicant and received offers from IMs,
      •   to allow creation of a new capacity request with all the necessary data,
      •   search for a route, select a desired route from the available options (results) and
check capacity for the selected route option,
      •   the list of created train information with all variants,
      •   the list of the path provided with linked train objects,
      •   price estimation for the path (connection to Sales module).
  The Frontend portal may look like it is presented on the image 29.
Figure 21 - Booking Frontend portal
  Applicants can search for a capacity according to different train parameters (speed, length,
axle weight, etc.), desired times and dates (departure, arrival, connection tolerance), routes,
waypoints and so on.
  The search will return the list of available routes between the searched locations, and the
user can select the desired route from the available options, and also check the capacity for the
selected route.
  The list of possible routes between entered origin and destination will be presented and Appli-
cants have possibility to select each and check the capacity on the route.
  According to the connection tolerance on the border, a parameter entered in the search, the
system will propose the route combination accordingly. Of course, the Applicants will have a
possibility to select a perfect combination of the proposed route options for them, and request
such a defined route immediately or save this search and selected combination for the later
work.
  The national particularities of the IMs shall be filled in as well, as it is presented in the follow-
ing image.
  In the prepared path request, Applicants can check the list of planned TCRs on the route, the
price estimation for this path (connection to the Sales module), see the whole path request in-
formation with the variants, linked train objects, calendar, involved IMs and partner Applicants
and so on.
  The presentation of the whole selected path and visualisation of TCRs that is affecting the
path, can be provided on the map overview.
  Consulting the national systems
  The Capacity Broker will use the provided request parameters (locations, departure, arrival)
and firstly check the pre-planned capacity that is provided to the central system and an over-
view on the possibility to fulfil the requirements will be provided. This capacities to the central
system will be provided by IMs from their pre-planned capacities submitted in their national sys-
tems.
  If the Applicants requests can be completely fulfilled or the automatically suggested path by
the Capacity Broker is fine to the Applicants, this information will be sent to IMs’ national sys-
tems with request to check the capacity nationally and provide the draft offer.
   Even it is a demand that IMs publish their capacities to the central system, still it is not real to
expect that all the capacities will be published. Therefore, there should be implemented a func-
tionality of the Capacity Broker to send automatic request to national systems (national Broker
systems) of the IMs who are affected by the request.
If there is no capacity provided to the central system or the proposed capacity doesn’t fit to the
Applicant, the request will be sent to the IMs’ national systems to provide an answer. In such a
situation the capacity shall be directly provided to the central system “on-line”.
  The national systems will be consulted in the following cases:
•     There is no capacity provided to the central system,
•     The received request does not fit to the provided capacity neither it is possible to find
the fitting paths inside the parametrized time offset that is acceptable to Applicants,
•     The proposal generated by the Capacity Broker is a very first draft information pro-
vided to the Applicant, but the final answer MUST be confirmed by IMs.
The IM national system (national “Broker” tool) can provide an answer by doing one of the fol-
lowing actions:
    •       automatically create a path, considering provided information and taking into account
national TCRs.
    •       check their national pre-planned capacity, that is not provided to the central system, to
prepare an offer.
    •       create a tailor-made capacity for the received request.
A created path proposal will be provided back to the central system and presented on the portal
to the Applicants. The created path will be sent automatically to those Applicants systems who
has it.
## 1.13.7 Centralized master data
 The algorithm itself doesn't have any master data. Everything is stored either in the Capacity
Hub (ECMT) or in the Path Request, Path Management (PCS) modules.
## 1.13.8 Integration options
  The algorithm itself doesn't have any particular need for integration.
1.13.9 Connection to other modules
  Details regarding the requirements are written in the modules or under the functionality, but
the high-level process shall look like the following:
   1. CB gets the request or wishes from Applicant via the Path Request Management module
   2. CB checks the available capacity products in the Capacity Hub inside the defined time
      constraint
   3. If there is not any capacity product available, the Broker checks its configuration
      whether there are IMs in the route who can provide automatic calculation
      a) If yes, CB asks running time calculation from the IMs (real-time time connection to
the IMs systems)
      b) If not, CB calculates the running time itself
   4. CB stores the information either received from the IMs or calculated itself in the Path
      Management
   5. The broker sends feasibility result, a proposal to the Applicant
  Minimal requirements
  Capacity Hub module
      •   Capacity Broker (CB) shall be able to combine catalogue paths, capacity bands in the
Capacity Hub according to the defined time constraint per segment (RNE BigData re-
quirement)
      •   CB shall be able to calculate "tailor-made" running time based on the defined
PlannedTrainTechnicalData and the infrastructure data. It shall be done either via RNE
Data Warehouse using real Big Data solution or apply traditional physics for running
time calculation.
      •   CB shall send a request to the DWH for checking historical data in planned train data.
CB shall do reliability check of the received estimation.
      •   CB shall apply buffer time in the "tailor-made" running time calculation.
      •   CB shall avoid conflicts in the "tailor-made" running time calculation with other ob-
jects (TCRs, already allocated paths).
      ▪
  Path Request Management module
      •   CB shall be able to receive feasibility study requests from the Path Request Manage-
ment.
      •   CB shall notify the Applicant about the completed feasibility study result.
  Path Management module
     •   CB shall gather all the information for the offer of the feasibility study request in Path
Management
 Additional requirements
     •   The algorithm to find the best fitting capacity according to the inquiry request by RUs
(will give information to RUs that their requests fit the available capacity or infor-
mation that there is a problem due to TCRs or similar)
     •   IMs national systems must be able to respond on the capacity inquiries in real-time
even if they did not publish the capacity product for the particular line or train charac-
teristic. More precisely, if the RU makes an inquiry in the CB that does not only take
into account the published capacity products, the IMs system must be able to answer
if there was the available capacity to be used for the tailor-made offer (or combination
of capacity product and tailor-made).
     •   The compilation and harmonisation of national paths at the hand over points
     •   The conflict resolution procedure (e.g. calculation of distance and running days in or-
der to define the priority value)
## 1.13.10Functionality
 Minimal requirements
 Workflow
     •   Path proposal preparation workflow shall be implemented in the CB.
     •   CB shall take the right running time calculation options based on the given workflow.
 Collection of capacity products
     •   Broker shall be able to combine catalogue path, capacity bands in the Capacity Hub
according to the defined time constraint per segment (RNE BigData requirement)
 Gathering running time estimation from IMs
     •   Broker shall be able to store the list of IMs or infrastructure where automatic running
time calculation is available nationally
     •   Broker shall contact automatically the national systems and request running time cal-
culation based
     •   Broker shall update the Path Management with the received answers
 Running time calculation via DWH
     •   Broker shall send a request to the DWH for checking historical data in planned train
data.
     •   Broker shall do reliability check of the received estimation.
  Own running time calculation
     •   In the worst case, the broker shall be able to do a calculation itself applying traditional
physics formulas extending it with a buffer time as usual in timetabling.
## 1.14 Sales (S)
  The Sales module does not exist as an application (only the CIS system exists, but it should be
technically re-developed due to its old IT framework). In the future, the functionality of this
module is foreseen to be implemented in the TTR IT Landscape as an additional module that will
contain the following groups of the functionality:
     •    Commercial conditions
     •    Charging information (current CIS functionality which provides charge estimates)
     •    Network Statement (NS) digitalization (to be developed as a separate tool)
   Summary
 Completeness:
  - Only a general overview of the module is described, and this module is still
    in preparation and not ready to be assessed                                         IM      RNE
TEG/SMO   TTR IT
 Open issues:                                     Reasons:
  - Commercial conditions and allocation
- There is no common decision neither proposal what
    rules
nor how it should be implemented
  - Charging information system functionality
- Feedback from Commercial Condition group is ex-
pected
 Plan to make this topic green:
  - After a decision on the Commercial conditions and allocation rules, a specification for implemen-
    tation will be prepared
  - The involvement of the CIS (Charging information system) in the TTR IT Landscape is not defined
    yet. Depends on the conclusion from the group responsible for the CIS implementation.
 Dependency:
  - Feedback from the Commercial Condition group
 Timeline:
  - More detailed description of this topic is planned for the 2nd TTR IT Landscape implementation step
    (after TT2025)
  -
 Implementation deadlines:
  - Will be implemented in the second implementation step
## 1.14.1 Centralized master data
   Currently, the CIS is a completely stand-alone application with its own database that is ful-
filled manually. The following information is stored:
      •   Countries, Locations, Lines, Line segments, Line categories
      •   Train parameters such as train types, traction types
      •   Additional parameters for the calculation specified by the IM
      •   CIS formula attributes
  The connection with a central master data is needed for the topological data.
  Minimal requirements
   The Sales module will not be considered as a minimal requirement of the TTR implementa-
tion.
  Additional requirements
  As the Sales module does not exist, the paragraph „Functionality“describes the possible
functionalities of the implementation.
## 1.14.2 Integration options
  Minimal requirements
Object updates
  Commercial conditions should be defined and implemented with its process
  Allocation rules should be defined
  Charging information defined, and the message to exchange the data with other modules
  NS digitalized data
Schema and message definition
  XSD proposal of charging information shall be defined for TAF/TAP TSI
  XSD proposal for the charging information handling (costs data) shall be defined for TAF/TAP
TSI
Endpoint and communication
  Common interface endpoint shall be opened for Sales module
  Sales module shall be ready to send and receive TAF/TAP TSI messages related to working
with the available objects
  As the Sales module will include information that shall be publicly available by EU law, access
to this module shall be granted to all interested users free of charge and if possible, without
user registration to decrease administrative effort.
## 1.14.3 Integration options
  Minimal requirements
  Path Request Management
     •   Charging information shall be provided for the information purposes
  Path Management
     •   Charging information shall be provided for the information purposes
  Applicant’s GUI
     •   The Sales module shall be integrated into the GUI
  Additional requirements
  Shall be defined later, when the final version of the Commercial Conditions document shall
be ready.
## 1.14.4 Functionality
  Minimal requirements
  A possible implementation of the functionality that currently exists in the CIS system. A mas-
ter data for this functionality shall be BigData module (from the topology point of view).
  Additional requirements
  The group responsible for Commercial Conditions shall define the functionality and the CIS
Change Control Board shall consider if and how this functionality could be added to the CIS. A
harmonized functionality of the Commercial conditions shall be related to the following:
      •   Path cancellation
      •   Non-usage of a path
      •   Cancellation of a partially non-used path
      •   Path modification
      •   Path alteration
Annex 1: Proposal of new objects and messages (complete XSD schema)
The summary of modification proposal of the existing objects and elements in the current TAF
TSI schema, as well as definition of new messages, can be found bellow:
New objects: CN as a capacity needs announcements, CM as a capacity model object, BA as a
capacity band, PP as a pre-arranged path, CP as catalogue path.
New elements:
New elements that will be added into the schema are in the line with new messages. Since, the
number of new elements will be longer, especially considering the Capacity Model Message,
new elements will not be listed separately.
Updated existing elements:
       - ObjectType of the Identifier was updated with new values
- TypeOfRequest values: 1 Path Study, 2 Ad-Hoc Path Request, 3 Path Modification/Al-
teration, 4 Annual Path Request, 5 Late Path Request, 6 Rolling Planning Path Request, 7 Ca-
pacity Bands, 8 Pre-arranged Paths, 9 Catalogue Paths
New messages:
       - TCRMessage: to exchange (send) the TCRs data with the TCR module.
       - TCRCanceledMessage: to cancel the TCR
       - TCRResponseMessage: shall be used as a result of TCR data update
       - CapacityModelMessage: to provide data on CNAs and CMOs
       - CapacityProductSearchMessage: shall be used to search all the capacity products
       - CapacityProductMessage: is the response to the search capacity product message
Messages are published on the JSG website.
Annex 2: Capacity product publication, Capacity product closure after publication, Capacity product capacity return, capacity product withdrawal
Since the images are too big to be shown in the document, there are attached as separate files.
The files can be found below:
 Capacity product publication                                                  Capacity product
publication v1.pdf
 Capacity product closure after publication                                     Capacity product
closure after publication v1.pdf
 Capacity product capacity return                                              Capacity product
capacity return v1.pdf
 Capacity product withdrawal                                                   Capacity product
withdrawal v1.pdf
Annex 3: TTR IT Message exchange – the first implementation step
                                                                  2024
Annex 4: TCR messages – XML and Excel specification
   Annex 4 - TCR
messages, XML and Excel specification.docx
Annex 5: Common interface “Step-by-step” wizard description
  Annex 5 - Common
Interface Step-by-step wizard description.docx
Annex 6: Common Interface API request description
  Annex 6 - Common
Interface API request description.docx
Annex 7: Requesting a capacity via TAF/TAP TSI
Annex 7 - Requesting
capacity via TAF-TSI.docx
Annex 8: Capacity Needs Announcements Excel file structure
 Annex 8 - Capacity
Needs Announcements Excel file structure.xlsx
Annex 9: TTR needed infrastructure data
 Austria Campus 3
 Jakov-Lind-Straße 5                 Phone: +43 1 907 62 72 00
 AT-1020 Vienna                      E-Mail: mailbox@rne.eu      www.rne.eu
                                  Add Document Title
   Annex 9 - TTR
needed infrastructure data.docx
Add any other information                       142
