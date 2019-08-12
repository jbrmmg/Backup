CREATE TABLE `backup` (
  `id` char(4) NOT NULL,
  `type` varchar(50) NOT NULL,
  `directory` varchar(300) NOT NULL,
  `artifact` varchar(100) NOT NULL,
  `backupname` varchar(35) NOT NULL,
  `filename` varchar(100) NOT NULL,
  `time` integer NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;