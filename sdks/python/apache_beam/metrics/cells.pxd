#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

cimport cython
cimport libc.stdint
from cpython.datetime cimport datetime


cdef class MetricCell(object):
  cdef object _lock
  cpdef bint update(self, value) except -1
  cdef datetime _start_time


cdef class CounterCell(MetricCell):
  cdef readonly libc.stdint.int64_t value

  @cython.locals(ivalue=libc.stdint.int64_t)
  cpdef bint update(self, value) except -1


# Not using AbstractMetricCell so that data can be typed.
cdef class DistributionCell(MetricCell):
  cdef readonly DistributionData data

  @cython.locals(ivalue=libc.stdint.int64_t)
  cdef inline bint _update(self, value) except -1


cdef class AbstractMetricCell(MetricCell):
  cdef readonly object data_class
  cdef public object data
  cdef bint _update_locked(self, value) except -1


cdef class GaugeCell(AbstractMetricCell):
  pass


cdef class StringSetCell(AbstractMetricCell):
  pass


cdef class BoundedTrieCell(AbstractMetricCell):
  pass


cdef class DistributionData(object):
  cdef readonly libc.stdint.int64_t sum
  cdef readonly libc.stdint.int64_t count
  cdef readonly libc.stdint.int64_t min
  cdef readonly libc.stdint.int64_t max


cdef class _BoundedTrieNode(object):
  cdef readonly libc.stdint.int64_t _size
  cdef readonly dict _children
  cdef readonly bint _truncated

cdef class BoundedTrieData(object):
  cdef readonly libc.stdint.int64_t _bound
  cdef readonly object _singleton
  cdef readonly _BoundedTrieNode _root
